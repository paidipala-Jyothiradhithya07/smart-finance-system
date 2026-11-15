package com.smartfinance.statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartfinance.config.CategoryRuleProperties;
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
import com.smartfinance.statement.category.RuleBasedCategorizer;
import com.smartfinance.statement.dedup.Fingerprints;
import com.smartfinance.statement.model.ParsedStatement;
import com.smartfinance.statement.model.ParsedTransaction;
import com.smartfinance.statement.normalize.DateOrder;

@ExtendWith(MockitoExtension.class)
class StatementImporterTest {

    @Mock
    private BankStatementRepository statementRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;

    @Captor
    private ArgumentCaptor<List<Transaction>> savedCaptor;

    private StatementImporter importer;

    @BeforeEach
    void setUp() {
        importer = new StatementImporter(statementRepository, transactionRepository, categoryRepository,
                userRepository, new RuleBasedCategorizer(new CategoryRuleProperties()));
    }

    // Stubs for the tests that really save something
    private void stubSaving() {
        when(categoryRepository.findByUserIsNullOrderByNameAsc()).thenReturn(List.of(
                category(1L, "Salary", CategoryType.INCOME),
                category(6L, "Food", CategoryType.EXPENSE),
                category(9L, "Rent", CategoryType.EXPENSE),
                category(16L, "Other", CategoryType.BOTH)));
        when(userRepository.getReferenceById(1L)).thenReturn(new User());
        when(statementRepository.saveAndFlush(any(BankStatement.class))).thenAnswer(invocation -> {
            BankStatement statement = invocation.getArgument(0);
            statement.setId(5L);
            return statement;
        });
        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void importsNewRowsAndSkipsRowsThatAreAlreadyStored() {
        stubSaving();
        List<ParsedTransaction> rows = List.of(
                row(2, "2026-01-01", "SALARY CREDIT", "50000.00", TransactionType.CREDIT, "70000.00"),
                row(3, "2026-01-03", "RENT PAYMENT", "15000.00", TransactionType.DEBIT, "55000.00"),
                row(4, "2026-01-05", "SWIGGY", "850.00", TransactionType.DEBIT, "54150.00"));
        String storedHash = Fingerprints.transactionHashes(rows).get(1);       // the rent row is already stored
        when(transactionRepository.findExistingDedupHashes(eq(1L), anyCollection()))
                .thenReturn(List.of(storedHash));

        StatementImporter.Outcome outcome = importer.save(1L, "filehash", statement(rows, 0));

        assertThat(outcome.imported()).isEqualTo(2);
        assertThat(outcome.duplicates()).extracting(ParsedTransaction::description).containsExactly("RENT PAYMENT");
        assertThat(outcome.balanceWarnings()).isEmpty();
        assertThat(outcome.statement().getImportedCount()).isEqualTo(2);
        assertThat(outcome.statement().getDuplicateCount()).isEqualTo(1);
        assertThat(outcome.statement().getStatus()).isEqualTo(StatementStatus.COMPLETED);

        verify(transactionRepository).saveAll(savedCaptor.capture());
        List<Transaction> saved = savedCaptor.getValue();
        assertThat(saved).extracting(Transaction::getDescription).containsExactly("SALARY CREDIT", "SWIGGY");
        assertThat(saved).extracting(t -> t.getCategory().getName()).containsExactly("Salary", "Food");
        assertThat(saved).extracting(Transaction::getRowSequence).containsExactly(1, 3);
        assertThat(saved).allSatisfy(t -> {
            assertThat(t.getSource()).isEqualTo(TransactionSource.BANK_STATEMENT);
            assertThat(t.getDedupHash()).hasSize(64);
        });
    }

    @Test
    void whenEverythingIsAlreadyStored_noStatementIsCreated() {
        List<ParsedTransaction> rows = List.of(
                row(2, "2026-01-01", "SALARY CREDIT", "50000.00", TransactionType.CREDIT, null),
                row(3, "2026-01-03", "RENT PAYMENT", "15000.00", TransactionType.DEBIT, null));
        when(transactionRepository.findExistingDedupHashes(eq(1L), anyCollection()))
                .thenReturn(Fingerprints.transactionHashes(rows));

        StatementImporter.Outcome outcome = importer.save(1L, "filehash", statement(rows, 0));

        assertThat(outcome.statement()).isNull();
        assertThat(outcome.imported()).isZero();
        assertThat(outcome.duplicates()).hasSize(2);
        verify(statementRepository, never()).saveAndFlush(any());
        verify(transactionRepository, never()).saveAll(anyList());
    }

    @Test
    void unknownDescriptionsFallBackToOther_andInvalidRowsMarkTheStatement() {
        stubSaving();
        when(transactionRepository.findExistingDedupHashes(eq(1L), anyCollection())).thenReturn(List.of());
        List<ParsedTransaction> rows = List.of(
                row(2, "2026-01-02", "XYZ TRADERS", "100.00", TransactionType.DEBIT, null));

        StatementImporter.Outcome outcome = importer.save(1L, "filehash", statement(rows, 2));

        assertThat(outcome.uncategorized()).isEqualTo(1);
        assertThat(outcome.importedMonths()).containsExactly(YearMonth.of(2026, 1));
        assertThat(outcome.statement().getStatus()).isEqualTo(StatementStatus.COMPLETED_WITH_ERRORS);
        assertThat(outcome.statement().getInvalidCount()).isEqualTo(2);

        verify(transactionRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue().get(0).getCategory().getName()).isEqualTo("Other");
    }

    // ---------- helpers ----------

    private static ParsedTransaction row(int rowNumber, String date, String description, String amount,
                                         TransactionType type, String balance) {
        return new ParsedTransaction(rowNumber, LocalDate.parse(date), description, new BigDecimal(amount), type,
                balance == null ? null : new BigDecimal(balance), null);
    }

    private static ParsedStatement statement(List<ParsedTransaction> rows, int invalidRows) {
        return new ParsedStatement("test.csv", FileType.CSV, 1, null, DateOrder.DAY_FIRST,
                rows.size() + invalidRows, 0, invalidRows, rows, List.of(), List.of());
    }

    private static Category category(Long id, String name, CategoryType type) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setType(type);
        return category;
    }
}