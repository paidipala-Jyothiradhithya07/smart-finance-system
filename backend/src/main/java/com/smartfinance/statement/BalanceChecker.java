package com.smartfinance.statement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.smartfinance.entity.enums.TransactionType;
import com.smartfinance.statement.model.ParsedTransaction;

public final class BalanceChecker {

    public record Result(int comparedPairs, int mismatches, List<String> warnings) {
    }

    private static final int MAX_WARNINGS = 10;

    private BalanceChecker() {
    }

    /**
     * For each pair of neighbouring rows that both have a balance, checks that
     * previous balance, plus a credit or minus a debit, equals this row's balance.
     * A mismatch usually means a missing row or a different row order. It is a warning, never an error.
     */
    public static Result check(List<ParsedTransaction> chronological) {
        int compared = 0;
        int mismatches = 0;
        List<String> warnings = new ArrayList<>();

        ParsedTransaction previous = null;
        for (ParsedTransaction current : chronological) {
            if (previous != null && previous.balance() != null && current.balance() != null) {
                compared++;
                boolean credit = current.type() == TransactionType.CREDIT;
                BigDecimal expected = credit
                        ? previous.balance().add(current.amount())
                        : previous.balance().subtract(current.amount());
                if (expected.compareTo(current.balance()) != 0) {
                    mismatches++;
                    if (warnings.size() < MAX_WARNINGS) {
                        warnings.add("Row " + current.rowNumber() + ": balance "
                                + current.balance().toPlainString() + " does not match the expected "
                                + expected.toPlainString() + " (previous balance "
                                + previous.balance().toPlainString() + (credit ? " plus " : " minus ")
                                + current.amount().toPlainString()
                                + "). Some rows may be missing, or the bank's row order may differ.");
                    }
                }
            }
            previous = current;
        }

        int hidden = mismatches - warnings.size();
        if (hidden > 0) {
            warnings.add("... and " + hidden + " more balance mismatches.");
        }
        return new Result(compared, mismatches, List.copyOf(warnings));
    }
}