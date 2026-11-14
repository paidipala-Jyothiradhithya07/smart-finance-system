package com.smartfinance.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import com.smartfinance.entity.enums.TransactionType;
import com.smartfinance.statement.mapping.ColumnMapping;
import com.smartfinance.statement.mapping.ColumnRole;
import com.smartfinance.statement.model.ParsedTransaction;
import com.smartfinance.statement.model.RowError;
import com.smartfinance.statement.normalize.AmountParser;
import com.smartfinance.statement.normalize.AmountParser.Hint;
import com.smartfinance.statement.normalize.AmountParser.ParsedAmount;
import com.smartfinance.statement.normalize.DateParser;
import com.smartfinance.statement.reader.RawRow;

public class RowNormalizer {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999.99");
    private static final int MIN_YEAR = 2000;
    private static final int MAX_DESCRIPTION = 500;
    private static final int MAX_REFERENCE = 100;

    // Opening and closing balance lines, totals, brought-forward lines
    private static final Pattern SUMMARY = Pattern.compile(
            "^(opening|closing) bal(ance)?\\b.*"
            + "|^balance (b f|c f|bf|cf|brought forward|carried forward).*"
            + "|^(b f|c f|bf|cf|brought forward|carried forward|balance forward)\\b.*"
            + "|^(grand )?total\\b.*"
            + "|^statement summary.*");

    private record Money(BigDecimal amount, TransactionType type) {
    }

    private final ColumnMapping mapping;
    private final DateParser dateParser;
    private final LocalDate today;

    public RowNormalizer(ColumnMapping mapping, DateParser dateParser, LocalDate today) {
        this.mapping = mapping;
        this.dateParser = dateParser;
        this.today = today;
    }

    public NormalizedRow normalize(RawRow row) {
        String dateText = text(row, ColumnRole.DATE);
        String description = clean(text(row, ColumnRole.DESCRIPTION), MAX_DESCRIPTION);
        boolean noAmounts = AmountParser.isBlank(text(row, ColumnRole.DEBIT))
                && AmountParser.isBlank(text(row, ColumnRole.CREDIT))
                && AmountParser.isBlank(text(row, ColumnRole.AMOUNT));

        // Lines that are not transactions: notes, totals, opening/closing balance lines
        if (dateText.isBlank()) {
            if (noAmounts || isSummary(description)) {
                return NormalizedRow.skipped();
            }
            return NormalizedRow.invalid(List.of(error(row, ColumnRole.DATE, "Date is missing")));
        }
        Optional<LocalDate> parsedDate = dateParser.parse(dateText);
        if (noAmounts && (parsedDate.isEmpty() || isSummary(description))) {
            return NormalizedRow.skipped();
        }

        List<RowError> errors = new ArrayList<>();

        LocalDate date = null;
        if (parsedDate.isEmpty()) {
            errors.add(error(row, ColumnRole.DATE, "'" + shorten(dateText) + "' is not a valid date"));
        } else {
            date = parsedDate.get();
            if (date.isAfter(today)) {
                errors.add(error(row, ColumnRole.DATE, "Date " + date + " is in the future"));
            } else if (date.getYear() < MIN_YEAR) {
                errors.add(error(row, ColumnRole.DATE, "Date " + date + " is before the year " + MIN_YEAR));
            }
        }

        if (description.isEmpty()) {
            errors.add(error(row, ColumnRole.DESCRIPTION, "Description is missing"));
        }

        Money money = null;
        if (noAmounts) {
            errors.add(error(row, firstAmountRole(), "No debit, credit or amount value in this row"));
        } else {
            money = resolveMoney(row, errors);
            if (money == null && errors.isEmpty()) {
                errors.add(error(row, firstAmountRole(), "Amount is missing"));
            }
        }

        BigDecimal balance = readBalance(row, errors);

        if (!errors.isEmpty()) {
            return NormalizedRow.invalid(errors);
        }

        String reference = clean(text(row, ColumnRole.REFERENCE), MAX_REFERENCE);
        return NormalizedRow.valid(new ParsedTransaction(
                row.rowNumber(), date, description, money.amount(), money.type(),
                balance, reference.isEmpty() ? null : reference));
    }

    // ---------- amount and type ----------

    private Money resolveMoney(RawRow row, List<RowError> errors) {
        if (mapping.has(ColumnRole.DEBIT) || mapping.has(ColumnRole.CREDIT)) {
            return fromDebitAndCredit(row, errors);
        }
        return fromSingleAmount(row, errors);
    }

    // Separate Debit and Credit columns (either one may be missing). The sign in these columns is ignored.
    private Money fromDebitAndCredit(RawRow row, List<RowError> errors) {
        int before = errors.size();
        ParsedAmount debit = parseCell(row, ColumnRole.DEBIT, errors);
        ParsedAmount credit = parseCell(row, ColumnRole.CREDIT, errors);
        if (errors.size() > before) {
            return null;
        }
        boolean hasDebit = debit != null && debit.value().signum() != 0;
        boolean hasCredit = credit != null && credit.value().signum() != 0;

        if (hasDebit && hasCredit) {
            errors.add(error(row, ColumnRole.DEBIT, "Both debit and credit have a value"));
            return null;
        }
        if (hasDebit) {
            return checked(debit.value().abs(), TransactionType.DEBIT, row, ColumnRole.DEBIT, errors);
        }
        if (hasCredit) {
            return checked(credit.value().abs(), TransactionType.CREDIT, row, ColumnRole.CREDIT, errors);
        }
        errors.add(error(row, firstAmountRole(), "Amount must be greater than zero"));
        return null;
    }

    // One Amount column. The type comes from a type column, then a Dr/Cr marker, then the sign.
    private Money fromSingleAmount(RawRow row, List<RowError> errors) {
        ParsedAmount parsed = parseCell(row, ColumnRole.AMOUNT, errors);
        if (parsed == null) {
            return null;
        }
        BigDecimal magnitude = parsed.value().abs();
        if (magnitude.signum() == 0) {
            errors.add(error(row, ColumnRole.AMOUNT, "Amount must be greater than zero"));
            return null;
        }

        TransactionType fromColumn = null;
        String typeText = text(row, ColumnRole.TYPE);
        if (!typeText.isBlank()) {
            fromColumn = interpretType(typeText);
            if (fromColumn == null) {
                errors.add(error(row, ColumnRole.TYPE, "Unrecognised transaction type '" + shorten(typeText) + "'"));
                return null;
            }
        }
        TransactionType fromMarker = parsed.hint() == null ? null
                : parsed.hint() == Hint.DEBIT ? TransactionType.DEBIT : TransactionType.CREDIT;

        if (fromColumn != null && fromMarker != null && fromColumn != fromMarker) {
            errors.add(error(row, ColumnRole.AMOUNT, "The type column and the Dr/Cr marker on the amount disagree"));
            return null;
        }
        TransactionType type = fromColumn != null ? fromColumn
                : fromMarker != null ? fromMarker
                : parsed.value().signum() < 0 ? TransactionType.DEBIT : TransactionType.CREDIT;

        return checked(magnitude, type, row, ColumnRole.AMOUNT, errors);
    }

    private Money checked(BigDecimal amount, TransactionType type, RawRow row, ColumnRole role,
                          List<RowError> errors) {
        if (amount.stripTrailingZeros().scale() > 2) {
            errors.add(error(row, role, "Amount has more than 2 decimal places"));
            return null;
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            errors.add(error(row, role, "Amount is too large"));
            return null;
        }
        return new Money(amount.setScale(2), type);
    }

    private ParsedAmount parseCell(RawRow row, ColumnRole role, List<RowError> errors) {
        String text = text(row, role);
        try {
            return AmountParser.parse(text).orElse(null);
        } catch (IllegalArgumentException ex) {
            errors.add(error(row, role, "'" + shorten(text) + "' is not a valid amount"));
            return null;
        }
    }

    private static TransactionType interpretType(String text) {
        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        return switch (normalized) {
            case "dr", "debit", "d", "debited", "withdrawal", "wd", "w" -> TransactionType.DEBIT;
            case "cr", "credit", "c", "credited", "deposit", "dep" -> TransactionType.CREDIT;
            default -> null;
        };
    }

    // ---------- balance ----------

    private BigDecimal readBalance(RawRow row, List<RowError> errors) {
        if (!mapping.has(ColumnRole.BALANCE)) {
            return null;
        }
        String text = text(row, ColumnRole.BALANCE);
        if (AmountParser.isBlank(text)) {
            return null;
        }
        try {
            Optional<ParsedAmount> parsed = AmountParser.parse(text);
            if (parsed.isEmpty()) {
                return null;
            }
            // A "Dr" balance means the account is overdrawn, so it is negative
            BigDecimal value = parsed.get().hint() == Hint.DEBIT
                    ? parsed.get().value().abs().negate()
                    : parsed.get().value();
            if (value.stripTrailingZeros().scale() > 2 || value.abs().compareTo(MAX_AMOUNT) > 0) {
                errors.add(error(row, ColumnRole.BALANCE, "Balance has too many decimals or is too large"));
                return null;
            }
            return value.setScale(2);
        } catch (IllegalArgumentException ex) {
            errors.add(error(row, ColumnRole.BALANCE, "'" + shorten(text) + "' is not a valid balance"));
            return null;
        }
    }

    // ---------- helpers ----------

    private String text(RawRow row, ColumnRole role) {
        return mapping.has(role) ? row.cell(mapping.index(role)) : "";
    }

    private RowError error(RawRow row, ColumnRole role, String message) {
        return new RowError(row.rowNumber(), mapping.header(role), message);
    }

    private ColumnRole firstAmountRole() {
        if (mapping.has(ColumnRole.DEBIT)) {
            return ColumnRole.DEBIT;
        }
        return mapping.has(ColumnRole.CREDIT) ? ColumnRole.CREDIT : ColumnRole.AMOUNT;
    }

    private static boolean isSummary(String description) {
        String normalized = description.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
        return SUMMARY.matcher(normalized).matches();
    }

    private static String clean(String value, int maxLength) {
        String collapsed = value.replaceAll("\\s+", " ").trim();
        return collapsed.length() > maxLength ? collapsed.substring(0, maxLength) : collapsed;
    }

    private static String shorten(String value) {
        return value.length() > 40 ? value.substring(0, 40) + "..." : value;
    }
}