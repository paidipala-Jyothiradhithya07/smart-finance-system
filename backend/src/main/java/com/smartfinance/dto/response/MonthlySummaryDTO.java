package com.smartfinance.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record MonthlySummaryDTO(
        int year,
        int month,
        String label,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal savings,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        boolean balanceAvailable,
        String openingBalanceSource,
        String closingBalanceSource,
        long transactionCount,
        List<String> warnings) {
}