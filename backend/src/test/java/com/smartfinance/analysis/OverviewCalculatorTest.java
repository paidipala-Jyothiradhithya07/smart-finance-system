package com.smartfinance.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;

class OverviewCalculatorTest {

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static MonthlyFigures month(String month, String income, String expense, long count,
                                        String opening, String closing) {
        BigDecimal in = bd(income);
        BigDecimal out = bd(expense);
        return new MonthlyFigures(YearMonth.parse(month), in, out, in.subtract(out), count,
                opening == null ? null : bd(opening),
                opening == null ? BalanceSource.UNAVAILABLE : BalanceSource.STATEMENT,
                closing == null ? null : bd(closing),
                closing == null ? BalanceSource.UNAVAILABLE : BalanceSource.STATEMENT,
                List.of());
    }

    private static List<MonthlyFigures> threeMonths() {
        return List.of(
                month("2026-01", "50000", "32000", 13, "20000", "38000"),
                month("2026-02", "55000", "35000", 12, "38000", "58000"),
                month("2026-03", "52000", "31000", 12, "58000", "79000"));
    }

    @Test
    void totalsAndAverages() {
        Overview overview = OverviewCalculator.calculate(threeMonths());

        assertThat(overview.monthCount()).isEqualTo(3);
        assertThat(overview.monthsWithTransactions()).isEqualTo(3);
        assertThat(overview.transactionCount()).isEqualTo(37);
        assertThat(overview.totalIncome()).isEqualByComparingTo("157000");
        assertThat(overview.totalExpense()).isEqualByComparingTo("98000");
        assertThat(overview.totalSavings()).isEqualByComparingTo("59000");
        assertThat(overview.averageMonthlyIncome()).isEqualByComparingTo("52333.33");
        assertThat(overview.averageMonthlyExpense()).isEqualByComparingTo("32666.67");
        assertThat(overview.averageMonthlySavings()).isEqualByComparingTo("19666.67");
    }

    @Test
    void highestMonths() {
        Overview overview = OverviewCalculator.calculate(threeMonths());

        assertThat(overview.highestSpendingMonth().month()).isEqualTo(YearMonth.of(2026, 2));
        assertThat(overview.highestSpendingMonth().amount()).isEqualByComparingTo("35000");
        assertThat(overview.highestIncomeMonth().month()).isEqualTo(YearMonth.of(2026, 2));
        assertThat(overview.highestIncomeMonth().amount()).isEqualByComparingTo("55000");
    }

    @Test
    void increasingTrend_withMonthByMonthChanges() {
        Overview overview = OverviewCalculator.calculate(threeMonths());

        assertThat(overview.savingsTrend()).isEqualTo(SavingsTrend.INCREASING);
        List<SavingsChange> changes = overview.savingsChanges();
        assertThat(changes.get(0).changeFromPrevious()).isNull();
        assertThat(changes.get(1).changeFromPrevious()).isEqualByComparingTo("2000");
        assertThat(changes.get(1).changePercent()).isEqualByComparingTo("11.11");
        assertThat(changes.get(2).changeFromPrevious()).isEqualByComparingTo("1000");
        assertThat(changes.get(2).changePercent()).isEqualByComparingTo("5.00");
    }

    @Test
    void decreasingTrend() {
        Overview overview = OverviewCalculator.calculate(List.of(
                month("2026-01", "500", "200", 1, null, null),
                month("2026-02", "500", "300", 1, null, null),
                month("2026-03", "500", "400", 1, null, null)));

        assertThat(overview.savingsTrend()).isEqualTo(SavingsTrend.DECREASING);
    }

    @Test
    void mixedTrend() {
        Overview overview = OverviewCalculator.calculate(List.of(
                month("2026-01", "500", "400", 1, null, null),
                month("2026-02", "500", "200", 1, null, null),
                month("2026-03", "500", "300", 1, null, null)));

        assertThat(overview.savingsTrend()).isEqualTo(SavingsTrend.MIXED);
    }

    @Test
    void stableTrend() {
        Overview overview = OverviewCalculator.calculate(List.of(
                month("2026-01", "500", "400", 1, null, null),
                month("2026-02", "500", "400", 1, null, null)));

        assertThat(overview.savingsTrend()).isEqualTo(SavingsTrend.STABLE);
    }

    @Test
    void singleMonth_hasInsufficientDataForATrend() {
        Overview overview = OverviewCalculator.calculate(List.of(month("2026-01", "500", "400", 1, null, null)));

        assertThat(overview.savingsTrend()).isEqualTo(SavingsTrend.INSUFFICIENT_DATA);
        assertThat(overview.savingsChanges()).hasSize(1);
    }

    @Test
    void noMonths_givesZerosAndNoDivisionByZero() {
        Overview overview = OverviewCalculator.calculate(List.of());

        assertThat(overview.monthCount()).isZero();
        assertThat(overview.totalIncome()).isEqualByComparingTo("0");
        assertThat(overview.averageMonthlyIncome()).isEqualByComparingTo("0");
        assertThat(overview.averageMonthlySavings()).isEqualByComparingTo("0");
        assertThat(overview.highestSpendingMonth()).isNull();
        assertThat(overview.highestIncomeMonth()).isNull();
        assertThat(overview.savingsTrend()).isEqualTo(SavingsTrend.INSUFFICIENT_DATA);
        assertThat(overview.openingBalance()).isNull();
        assertThat(overview.closingBalance()).isNull();
    }

    @Test
    void zeroSavingsBefore_meansNoPercentageChange() {
        Overview overview = OverviewCalculator.calculate(List.of(
                month("2026-01", "400", "400", 1, null, null),
                month("2026-02", "500", "400", 1, null, null)));

        SavingsChange change = overview.savingsChanges().get(1);
        assertThat(change.changeFromPrevious()).isEqualByComparingTo("100");
        assertThat(change.changePercent()).isNull();
    }

    @Test
    void emptyMonthsCountInTheAverage_andTiesKeepTheEarliestMonth() {
        Overview overview = OverviewCalculator.calculate(List.of(
                month("2026-01", "1000", "500", 2, "100", "600"),
                month("2026-02", "0", "0", 0, "600", "600"),
                month("2026-03", "1000", "500", 2, "600", "1100")));

        assertThat(overview.monthsWithTransactions()).isEqualTo(2);
        assertThat(overview.averageMonthlyIncome()).isEqualByComparingTo("666.67");   // 2000 / 3 months
        assertThat(overview.highestSpendingMonth().month()).isEqualTo(YearMonth.of(2026, 1));
        assertThat(overview.openingBalance()).isEqualByComparingTo("100");
        assertThat(overview.closingBalance()).isEqualByComparingTo("1100");
    }
}