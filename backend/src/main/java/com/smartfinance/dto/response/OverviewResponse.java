package com.smartfinance.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record OverviewResponse(
        String currency,
        String from,
        String to,
        int monthsInRange,
        int monthsWithTransactions,
        long transactionCount,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal totalSavings,
        BigDecimal averageMonthlyIncome,
        BigDecimal averageMonthlyExpense,
        BigDecimal averageMonthlySavings,
        MonthAmountDTO highestSpendingMonth,
        MonthAmountDTO highestIncomeMonth,
        String savingsTrend,
        List<SavingsChangeDTO> savingsChanges,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        List<String> warnings) {
}