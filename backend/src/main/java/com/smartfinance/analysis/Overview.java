package com.smartfinance.analysis;

import java.math.BigDecimal;
import java.util.List;

public record Overview(
        int monthCount,
        int monthsWithTransactions,
        long transactionCount,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal totalSavings,
        BigDecimal averageMonthlyIncome,
        BigDecimal averageMonthlyExpense,
        BigDecimal averageMonthlySavings,
        MonthAmount highestSpendingMonth,
        MonthAmount highestIncomeMonth,
        SavingsTrend savingsTrend,
        List<SavingsChange> savingsChanges,
        BigDecimal openingBalance,
        BigDecimal closingBalance) {
}