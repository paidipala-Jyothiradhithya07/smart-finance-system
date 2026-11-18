package com.smartfinance.analysis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.smartfinance.entity.enums.TransactionType;

/**
 * The central business logic. Pure Java: it gets numbers in and gives numbers out, so it is easy to test.
 * Every month is calculated on its own, then balances are linked from one month to the next.
 */
public final class MonthlySummaryCalculator {

    private MonthlySummaryCalculator() {
    }

    /**
     * @param first  first month to calculate (inclusive)
     * @param last   last month to calculate (inclusive). Months without data are included, with zeros.
     * @param totals income, expense and row count per month (a month may be missing)
     * @param points bank rows that have a balance, already in true time order
     */
    public static List<MonthlyFigures> calculate(YearMonth first, YearMonth last,
                                                  List<MonthTotals> totals, List<BalancePoint> points) {
        Map<YearMonth, MonthTotals> totalsByMonth = new HashMap<>();
        for (MonthTotals item : totals) {
            totalsByMonth.put(item.month(), item);
        }
        Map<YearMonth, List<BalancePoint>> pointsByMonth = new HashMap<>();
        for (BalancePoint point : points) {
            pointsByMonth.computeIfAbsent(YearMonth.from(point.date()), key -> new ArrayList<>()).add(point);
        }

        List<MonthlyFigures> result = new ArrayList<>();
        BigDecimal previousClosing = null;
        for (YearMonth month = first; !month.isAfter(last); month = month.plusMonths(1)) {
            MonthTotals monthTotals = totalsByMonth.getOrDefault(month, MonthTotals.empty(month));
            List<BalancePoint> monthPoints = pointsByMonth.getOrDefault(month, List.of());
            MonthlyFigures figures = figuresFor(monthTotals, monthPoints, previousClosing);
            result.add(figures);
            previousClosing = figures.closingBalance();
        }
        return result;
    }

    private static MonthlyFigures figuresFor(MonthTotals totals, List<BalancePoint> points,
                                             BigDecimal previousClosing) {
        BigDecimal income = money(totals.income());
        BigDecimal expense = money(totals.expense());
        BigDecimal savings = income.subtract(expense);
        long count = totals.transactionCount();
        List<String> warnings = new ArrayList<>();

        BigDecimal opening;
        BalanceSource openingSource;
        BigDecimal closing;
        BalanceSource closingSource;

        if (!points.isEmpty()) {
            // The bank's own figures win
            opening = money(points.get(0).balanceBefore());
            closing = money(points.get(points.size() - 1).balanceAfter());
            openingSource = BalanceSource.STATEMENT;
            closingSource = BalanceSource.STATEMENT;

            BigDecimal credits = BigDecimal.ZERO;
            BigDecimal debits = BigDecimal.ZERO;
            for (BalancePoint point : points) {
                if (point.type() == TransactionType.CREDIT) {
                    credits = credits.add(point.amount());
                } else {
                    debits = debits.add(point.amount());
                }
            }
            BigDecimal expected = opening.add(credits).subtract(debits);
            if (expected.compareTo(closing) != 0) {
                warnings.add("The bank balances in this month do not add up: opening " + plain(opening)
                        + ", plus credits " + plain(credits) + ", minus debits " + plain(debits)
                        + " gives " + plain(expected) + ", but the last balance in the statement is "
                        + plain(closing) + ". Some rows may be missing, or the bank's row order may differ.");
            }

            long withoutBalance = count - points.size();
            if (withoutBalance > 0) {
                warnings.add(withoutBalance + (withoutBalance == 1 ? " transaction" : " transactions")
                        + " in this month " + (withoutBalance == 1 ? "has" : "have")
                        + " no balance information (for example manually entered ones). They are included in "
                        + "income, expenses and savings, but the opening and closing balances come only from "
                        + "the bank statement.");
            }
            if (previousClosing != null && previousClosing.compareTo(opening) != 0) {
                warnings.add("The opening balance " + plain(opening) + " differs from the previous month's "
                        + "closing balance " + plain(previousClosing) + ". A statement or a month may be missing.");
            }
        } else if (previousClosing != null) {
            opening = previousClosing;
            openingSource = BalanceSource.CARRIED_FORWARD;
            if (count == 0) {
                closing = previousClosing;                       // nothing happened, so nothing changed
                closingSource = BalanceSource.CARRIED_FORWARD;
            } else {
                closing = previousClosing.add(income).subtract(expense);
                closingSource = BalanceSource.CALCULATED;
                warnings.add("This month has no balance information, so the closing balance was calculated "
                        + "as the opening balance plus income minus expenses.");
            }
        } else {
            opening = null;
            closing = null;
            openingSource = BalanceSource.UNAVAILABLE;
            closingSource = BalanceSource.UNAVAILABLE;
            if (count > 0) {
                warnings.add("No balance information is available for this month, so the opening and "
                        + "closing balances cannot be determined.");
            }
        }

        return new MonthlyFigures(totals.month(), income, expense, savings, count,
                opening, openingSource, closing, closingSource, List.copyOf(warnings));
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String plain(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}