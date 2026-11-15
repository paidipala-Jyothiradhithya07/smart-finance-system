package com.smartfinance.statement;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.smartfinance.entity.BankStatement;
import com.smartfinance.entity.Category;
import com.smartfinance.entity.Transaction;
import com.smartfinance.entity.User;
import com.smartfinance.entity.enums.CategoryType;
import com.smartfinance.entity.enums.StatementStatus;
import com.smartfinance.entity.enums.TransactionSource;
import com.smartfinance.entity.enums.TransactionType;
import com.smartfinance.repository.BankStatementRepository;
import com.smartfinance.repository.CategoryRepository;
import com.smartfinance.repository.TransactionRepository;
import com.smartfinance.repository.UserRepository;
import com.smartfinance.statement.category.TransactionCategorizer;
import com.smartfinance.statement.dedup.Fingerprints;
import com.smartfinance.statement.model.ParsedStatement;
import com.smartfinance.statement.model.ParsedTransaction;

/**
 * Saves one parsed statement. It is a separate class from the upload service on purpose: Spring applies
 * @Transactional through a proxy, which only works when another bean calls the method.
 */
@Component
public class StatementImporter {

    private static final int HASH_QUERY_CHUNK = 500;

    public record Outcome(BankStatement statement,
                          int imported,
                          List<ParsedTransaction> duplicates,
                          int uncategorized,
                          List<String> balanceWarnings,
                          Set<YearMonth> importedMonths) {
    }

    private final BankStatementRepository statementRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionCategorizer categorizer;

    public StatementImporter(BankStatementRepository statementRepository,
                             TransactionRepository transactionRepository,
                             CategoryRepository categoryRepository,
                             UserRepository userRepository,
                             TransactionCategorizer categorizer) {
        this.statementRepository = statementRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.categorizer = categorizer;
    }

    @Transactional
    public Outcome save(Long userId, String fileHash, ParsedStatement parsed) {
        List<ParsedTransaction> ordered = ChronologicalOrder.sort(parsed.transactions());
        List<String> hashes = Fingerprints.transactionHashes(ordered);
        Set<String> known = findKnownHashes(userId, hashes);
        BalanceChecker.Result balance = BalanceChecker.check(ordered);

        List<ParsedTransaction> duplicates = new ArrayList<>();
        List<Integer> newIndexes = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            if (known.contains(hashes.get(i))) {
                duplicates.add(ordered.get(i));
            } else {
                newIndexes.add(i);
            }
        }

        // Everything in this file is already stored: no statement record is created
        if (newIndexes.isEmpty()) {
            return new Outcome(null, 0, duplicates, 0, balance.warnings(), Set.of());
        }

        Map<String, Category> systemCategories = categoryRepository.findByUserIsNullOrderByNameAsc().stream()
                .collect(Collectors.toMap(c -> c.getName().toLowerCase(Locale.ROOT), Function.identity()));
        Category fallback = systemCategories.get(TransactionCategorizer.FALLBACK_CATEGORY.toLowerCase(Locale.ROOT));
        if (fallback == null) {
            throw new IllegalStateException("The default category 'Other' is missing from the database");
        }

        User owner = userRepository.getReferenceById(userId);

        BankStatement statement = new BankStatement();
        statement.setUser(owner);
        statement.setFileName(parsed.fileName());
        statement.setFileHash(fileHash);
        statement.setTotalRows(parsed.totalDataRows());
        statement.setImportedCount(newIndexes.size());
        statement.setDuplicateCount(duplicates.size());
        statement.setInvalidCount(parsed.invalidRows());
        statement.setPeriodStart(parsed.periodStart());
        statement.setPeriodEnd(parsed.periodEnd());
        statement.setStatus(parsed.invalidRows() > 0
                ? StatementStatus.COMPLETED_WITH_ERRORS : StatementStatus.COMPLETED);
        statement = statementRepository.saveAndFlush(statement);

        List<Transaction> toSave = new ArrayList<>(newIndexes.size());
        Set<YearMonth> months = new TreeSet<>();
        int uncategorized = 0;

        for (int index : newIndexes) {
            ParsedTransaction row = ordered.get(index);
            Category category = chooseCategory(row, systemCategories, fallback);
            if (category == fallback) {
                uncategorized++;
            }

            Transaction transaction = new Transaction();
            transaction.setUser(owner);
            transaction.setBankStatement(statement);
            transaction.setCategory(category);
            transaction.setTransactionDate(row.date());
            transaction.setDescription(row.description());
            transaction.setAmount(row.amount());
            transaction.setType(row.type());
            transaction.setBalanceAfter(row.balance());
            transaction.setPaymentMethod(PaymentMethodDetector.detect(row.description(), row.reference()));
            transaction.setSource(TransactionSource.BANK_STATEMENT);
            transaction.setReferenceNo(row.reference());
            transaction.setRowSequence(index + 1);          // position in true time order within this statement
            transaction.setDedupHash(hashes.get(index));
            toSave.add(transaction);
            months.add(YearMonth.from(row.date()));
        }

        transactionRepository.saveAll(toSave);
        transactionRepository.flush();

        return new Outcome(statement, toSave.size(), duplicates, uncategorized, balance.warnings(), months);
    }

    // Asks the database about the fingerprints in small groups, so the query stays small
    private Set<String> findKnownHashes(Long userId, List<String> hashes) {
        Set<String> known = new HashSet<>();
        for (int from = 0; from < hashes.size(); from += HASH_QUERY_CHUNK) {
            List<String> chunk = hashes.subList(from, Math.min(from + HASH_QUERY_CHUNK, hashes.size()));
            known.addAll(transactionRepository.findExistingDedupHashes(userId, chunk));
        }
        return known;
    }

    private Category chooseCategory(ParsedTransaction row, Map<String, Category> systemCategories,
                                    Category fallback) {
        String suggested = categorizer.suggestCategoryName(row.description(), row.type());
        Category category = suggested == null ? null : systemCategories.get(suggested.toLowerCase(Locale.ROOT));
        if (category == null || !compatible(category.getType(), row.type())) {
            return fallback;
        }
        return category;
    }

    private static boolean compatible(CategoryType categoryType, TransactionType type) {
        return categoryType == CategoryType.BOTH
                || (type == TransactionType.CREDIT && categoryType == CategoryType.INCOME)
                || (type == TransactionType.DEBIT && categoryType == CategoryType.EXPENSE);
    }
}