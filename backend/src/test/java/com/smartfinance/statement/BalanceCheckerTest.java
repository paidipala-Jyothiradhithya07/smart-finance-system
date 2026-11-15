package com.smartfinance.statement;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.smartfinance.entity.enums.TransactionType;
import com.smartfinance.statement.model.ParsedTransaction;

class BalanceCheckerTest {

    private static ParsedTransaction tx(int row, String amount, TransactionType type, String balance) {
        return new ParsedTransaction(row, LocalDate.of(2026, 1, row), "X", new BigDecimal(amount), type,
                balance == null ? null : new BigDecimal(balance), null);
    }

    @Test
    void aConsistentChainHasNoWarnings() {
        BalanceChecker.Result result = BalanceChecker.check(List.of(
                tx(1, "50000.00", TransactionType.CREDIT, "70000.00"),
                tx(2, "15000.00", TransactionType.DEBIT, "55000.00"),
                tx(3, "850.00", TransactionType.DEBIT, "54150.00")));

        assertThat(result.comparedPairs()).isEqualTo(2);
        assertThat(result.mismatches()).isZero();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void aBrokenBalanceIsReportedWithItsRowNumber() {
        BalanceChecker.Result result = BalanceChecker.check(List.of(
                tx(1, "500.00", TransactionType.DEBIT, "9500.00"),
                tx(2, "400.00", TransactionType.DEBIT, "8000.00")));

        assertThat(result.mismatches()).isEqualTo(1);
        assertThat(result.warnings()).hasSize(1);
        assertThat(result.warnings().get(0))
                .startsWith("Row 2:")
                .contains("expected 9100.00");
    }

    @Test
    void creditsAreAddedToThePreviousBalance() {
        BalanceChecker.Result result = BalanceChecker.check(List.of(
                tx(1, "100.00", TransactionType.DEBIT, "900.00"),
                tx(2, "50.00", TransactionType.CREDIT, "950.00")));

        assertThat(result.mismatches()).isZero();
    }

    @Test
    void rowsWithoutABalanceAreSkipped() {
        BalanceChecker.Result result = BalanceChecker.check(List.of(
                tx(1, "100.00", TransactionType.DEBIT, null),
                tx(2, "50.00", TransactionType.DEBIT, "700.00"),
                tx(3, "50.00", TransactionType.DEBIT, null)));

        assertThat(result.comparedPairs()).isZero();
        assertThat(result.warnings()).isEmpty();
    }
}