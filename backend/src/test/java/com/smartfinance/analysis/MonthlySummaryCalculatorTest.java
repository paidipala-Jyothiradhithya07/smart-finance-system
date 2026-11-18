package com.smartfinance.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.smartfinance.entity.enums.TransactionType;

class MonthlySummaryCalculatorTest {

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static MonthTotals totals(String month, String income, String expense, long count) {
        return new MonthTotals(YearMonth.parse(month), bd(income), bd(expense), count);
    }

    /** Bank rows with a running balance. "+500" is a credit, "-200" a debit. One row per day. */
    private static List<BalancePoint> chain(String month, String opening, String... movements) {
        LocalDate day = YearMonth.parse(month).atDay(1);
        BigDecimal balance = bd(opening);
        List<BalancePoint> points = new ArrayList<>();
        for (String movement : movements) {
            boolean credit = movement.startsWith("+");
            BigDecimal amount = bd(movement.substring(1));
            balance = credit ? balance.add(amount) : balance.subtract(amount);
            points.add(new BalancePoint(day, credit ? TransactionType.CREDIT : TransactionType.DEBIT, amount, balance));
            day = day.plusDays(1);
        }
        return points;
    }

    private static void assertMonth(MonthlyFigures f, String month, String income, String expense,
                                    String savings, String opening, String closing, long count) {
        assertThat(f.month()).isEqualTo(YearMonth.parse(month));
        assertThat(f.income()).isEqualByComparingTo(income);
        assertThat(f.expense()).isEqualByComparingTo(expense);
        assertThat(f.savings()).isEqualByComparingTo(savings);
        assertThat(f.openingBalance()).isEqualByComparingTo(opening);
        assertThat(f.closingBalance()).isEqualByComparingTo(closing);
        assertThat(f.transactionCount()).isEqualTo(count);
    }

    @Test
    void threeMonths_matchTheWorkedExample() {
        List<MonthTotals> totals = List.of(
                totals("2026-01", "50000", "32000", 13),
                totals("2026-02", "55000", "35000", 12),
                totals("2026-03", "52000", "31000", 12));
        List<BalancePoint> points = new ArrayList<>();
        points.addAll(chain("2026-01", "20000", "+50000", "-15000", "-850", "-2200", "-500", "-3400",
                "-2500", "-649", "-1200", "-780", "-2400", "-1321", "-1200"));
        points.addAll(chain("2026-02", "38000", "+55000", "-15000", "-920", "-2350", "-640", "-3650",
                "-1450", "-4200", "-649", "-850", "-2500", "-2791"));
        points.addAll(chain("2026-03", "58000", "+50000", "-15000", "-880", "-2100", "-720", "-3300",
                "-2251", "-649", "-1100", "-3000", "-2000", "+2000"));

        List<MonthlyFigures> result = MonthlySummaryCalculator.calculate(
                YearMonth.of(2026, 1), YearMonth.of(2026, 3), totals, points);

        assertThat(result).hasSize(3);
        assertMonth(result.get(0), "2026-01", "50000", "32000", "18000", "20000", "38000", 13);
        assertMonth(result.get(1), "2026-02", "55000", "35000", "20000", "38000", "58000", 12);
        assertMonth(result.get(2), "2026-03", "52000", "31000", "21000", "58000", "79000", 12);
        assertThat(result).allSatisfy(f -> {
            assertThat(f.warnings()).isEmpty();
            assertThat(f.openingSource()).isEqualTo(BalanceSource.STATEMENT);
            assertThat(f.closingSource()).isEqualTo(BalanceSource.STATEMENT);
            assertThat(f.balanceAvailable()).isTrue();
        });
    }

    @Test
    void monthWithNoTransactions_carriesTheBalanceForward() {
        List<MonthTotals> totals = List.of(totals("2026-01", "500", "200", 2), totals("2026-03", "50", "100", 2));
        List<BalancePoint> points = new ArrayList<>();
        points.addAll(chain("2026-01", "1000", "+500", "-200"));
        points.addAll(chain("2026-03", "1300", "-100", "+50"));

        List<MonthlyFigures> result = MonthlySummaryCalculator.calculate(
                YearMonth.of(2026, 1), YearMonth.of(2026, 3), totals, points);

        MonthlyFigures february = result.get(1);
        assertMonth(february, "2026-02", "0", "0", "0", "1300", "1300", 0);
        assertThat(february.openingSource()).isEqualTo(BalanceSource.CARRIED_FORWARD);
        assertThat(february.closingSource()).isEqualTo(BalanceSource.CARRIED_FORWARD);
        assertThat(february.warnings()).isEmpty();
        assertMonth(result.get(2), "2026-03", "50", "100", "-50", "1300", "1250", 2);
        assertThat(result.get(2).warnings()).isEmpty();
    }

    @Test
    void monthWithoutBalanceInformation_isCalculatedFromThePreviousClosing() {
        List<MonthTotals> totals = List.of(totals("2026-01", "500", "200", 2), totals("2026-02", "100", "40", 2));

        List<MonthlyFigures> result = MonthlySummaryCalculator.calculate(
                YearMonth.of(2026, 1), YearMonth.of(2026, 2), totals, chain("2026-01", "1000", "+500", "-200"));

        MonthlyFigures february = result.get(1);
        assertThat(february.openingBalance()).isEqualByComparingTo("1300");
        assertThat(february.closingBalance()).isEqualByComparingTo("1360");
        assertThat(february.openingSource()).isEqualTo(BalanceSource.CARRIED_FORWARD);
        assertThat(february.closingSource()).isEqualTo(BalanceSource.CALCULATED);
        assertThat(february.warnings()).hasSize(1);
        assertThat(february.warnings().get(0)).contains("calculated");
    }

    @Test
    void noBalanceInformationAtAll_leavesBalancesUnavailable() {
        List<MonthlyFigures> result = MonthlySummaryCalculator.calculate(
                YearMonth.of(2026, 1), YearMonth.of(2026, 1), List.of(totals("2026-01", "500", "200", 2)), List.of());

        MonthlyFigures january = result.get(0);
        assertThat(january.openingBalance()).isNull();
        assertThat(january.closingBalance()).isNull();
        assertThat(january.balanceAvailable()).isFalse();
        assertThat(january.openingSource()).isEqualTo(BalanceSource.UNAVAILABLE);
        assertThat(january.closingSource()).isEqualTo(BalanceSource.UNAVAILABLE);
        assertThat(january.savings()).isEqualByComparingTo("300");
        assertThat(january.warnings().get(0)).contains("cannot be determined");
    }

    @Test
    void transactionsWithoutBalance_areCountedInSavingsButWarnedAbout() {
        // 600 income and 3 rows, but only 2 of the rows (500 and 200) came from the bank
        List<MonthlyFigures> result = MonthlySummaryCalculator.calculate(
                YearMonth.of(2026, 1), YearMonth.of(2026, 1),
                List.of(totals("2026-01", "600", "200", 3)), chain("2026-01", "1000", "+500", "-200"));

        MonthlyFigures january = result.get(0);
        assertThat(january.savings()).isEqualByComparingTo("400");
        assertThat(january.closingBalance()).isEqualByComparingTo("1300");
        assertThat(january.warnings()).hasSize(1);
        assertThat(january.warnings().get(0)).contains("no balance information");
    }

    @Test
    void openingThatDoesNotMatchThePreviousClosing_isWarnedAbout() {
        List<BalancePoint> points = new ArrayList<>();
        points.addAll(chain("2026-01", "1000", "+500", "-200"));
        points.addAll(chain("2026-02", "1000", "-100"));

        List<MonthlyFigures> result = MonthlySummaryCalculator.calculate(
                YearMonth.of(2026, 1), YearMonth.of(2026, 2),
                List.of(totals("2026-01", "500", "200", 2), totals("2026-02", "0", "100", 1)), points);

        assertThat(result.get(1).warnings()).hasSize(1);
        assertThat(result.get(1).warnings().get(0)).contains("differs from the previous month's closing balance");
    }

    @Test
    void aGapInTheBankBalances_isWarnedAbout() {
        // opening 1000, +500 should give 1500, -200 should give 1300, but the bank says 1000
        List<BalancePoint> points = List.of(
                new BalancePoint(LocalDate.of(2026, 1, 1), TransactionType.CREDIT, bd("500"), bd("1500")),
                new BalancePoint(LocalDate.of(2026, 1, 2), TransactionType.DEBIT, bd("200"), bd("1000")));

        List<MonthlyFigures> result = MonthlySummaryCalculator.calculate(
                YearMonth.of(2026, 1), YearMonth.of(2026, 1), List.of(totals("2026-01", "500", "200", 2)), points);

        assertThat(result.get(0).warnings()).hasSize(1);
        assertThat(result.get(0).warnings().get(0)).contains("do not add up");
    }

    @Test
    void spendingMoreThanEarning_givesNegativeSavings() {
        List<MonthlyFigures> result = MonthlySummaryCalculator.calculate(
                YearMonth.of(2026, 1), YearMonth.of(2026, 1), List.of(totals("2026-01", "100", "300", 2)), List.of());

        assertThat(result.get(0).savings()).isEqualByComparingTo("-200");
    }

    @Test
    void monthsInDifferentYears_stayApart() {
        List<MonthlyFigures> result = MonthlySummaryCalculator.calculate(
                YearMonth.of(2025, 12), YearMonth.of(2026, 1),
                List.of(totals("2025-12", "1000", "400", 3), totals("2026-01", "2000", "500", 4)), List.of());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).month()).isEqualTo(YearMonth.of(2025, 12));
        assertThat(result.get(0).income()).isEqualByComparingTo("1000");
        assertThat(result.get(1).month()).isEqualTo(YearMonth.of(2026, 1));
        assertThat(result.get(1).income()).isEqualByComparingTo("2000");
    }

    @Test
    void aMissingMiddleMonth_isFlaggedByTheNextOpeningBalance() {
        List<BalancePoint> points = new ArrayList<>();
        points.addAll(chain("2026-01", "1000", "+500", "-200"));          // closes at 1300
        points.addAll(chain("2026-03", "1800", "-100"));                   // opens at 1800: February is missing

        List<MonthlyFigures> result = MonthlySummaryCalculator.calculate(
                YearMonth.of(2026, 1), YearMonth.of(2026, 3),
                List.of(totals("2026-01", "500", "200", 2), totals("2026-03", "0", "100", 1)), points);

        assertThat(result.get(1).warnings()).isEmpty();
        assertThat(result.get(2).warnings()).hasSize(1);
        assertThat(result.get(2).warnings().get(0)).contains("A statement or a month may be missing");
    }
}
