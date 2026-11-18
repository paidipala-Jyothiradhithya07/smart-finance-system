package com.smartfinance.analysis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class OverviewCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private OverviewCalculator() {
    }

    /** Summarises the months that were asked for. Months with no transactions count as zero. */
    public static Overview calculate(List<MonthlyFigures> months) {
        BigDecimal income = BigDecimal.ZERO.setScale(2);
        BigDecimal expense = BigDecimal.ZERO.setScale(2);
        long transactions = 0;
        int monthsWithTransactions = 0;
        MonthAmount highestSpending = null;
        MonthAmount highestIncome = null;

        for (MonthlyFigures month : months) {
            income = income.add(month.income());
            expense = expense.add(month.expense());
            transactions += month.transactionCount();
            if (month.transactionCount() > 0) {
                monthsWithTransactions++;
            }
            // A strict "greater than" keeps the earliest month when two months tie
            if (month.expense().signum() > 0
                    && (highestSpending == null || month.expense().compareTo(highestSpending.amount()) > 0)) {
                highestSpending = new MonthAmount(month.month(), month.expense());
            }
            if (month.income().signum() > 0
                    && (highestIncome == null || month.income().compareTo(highestIncome.amount()) > 0)) {
                highestIncome = new MonthAmount(month.month(), month.income());
            }
        }

        BigDecimal savings = income.subtract(expense);
        int count = months.size();
        List<SavingsChange> changes = savingsChanges(months);

        BigDecimal opening = count == 0 ? null : months.get(0).openingBalance();
        BigDecimal closing = count == 0 ? null : months.get(count - 1).closingBalance();

        return new Overview(count, monthsWithTransactions, transactions,
                income, expense, savings,
                average(income, count), average(expense, count), average(savings, count),
                highestSpending, highestIncome, trendOf(changes), changes, opening, closing);
    }

    private static BigDecimal average(BigDecimal total, int count) {
        if (count == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private static List<SavingsChange> savingsChanges(List<MonthlyFigures> months) {
        List<SavingsChange> changes = new ArrayList<>();
        BigDecimal previous = null;
        for (MonthlyFigures month : months) {
            if (previous == null) {
                changes.add(new SavingsChange(month.month(), month.savings(), null, null));
            } else {
                BigDecimal change = month.savings().subtract(previous);
                BigDecimal percent = previous.signum() == 0
                        ? null    // a percentage of zero is meaningless
                        : change.multiply(HUNDRED).divide(previous.abs(), 2, RoundingMode.HALF_UP);
                changes.add(new SavingsChange(month.month(), month.savings(), change, percent));
            }
            previous = month.savings();
        }
        return List.copyOf(changes);
    }

    private static SavingsTrend trendOf(List<SavingsChange> changes) {
        if (changes.size() < 2) {
            return SavingsTrend.INSUFFICIENT_DATA;
        }
        boolean up = false;
        boolean down = false;
        for (int i = 1; i < changes.size(); i++) {
            int direction = changes.get(i).changeFromPrevious().signum();
            if (direction > 0) {
                up = true;
            } else if (direction < 0) {
                down = true;
            }
        }
        if (up && down) {
            return SavingsTrend.MIXED;
        }
        if (up) {
            return SavingsTrend.INCREASING;
        }
        return down ? SavingsTrend.DECREASING : SavingsTrend.STABLE;
    }
}