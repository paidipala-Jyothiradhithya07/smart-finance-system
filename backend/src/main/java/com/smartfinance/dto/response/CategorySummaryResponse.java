package com.smartfinance.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CategorySummaryResponse(
        String currency,
        String from,
        String to,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        List<CategoryAmountDTO> expenseCategories,
        List<CategoryAmountDTO> incomeCategories) {
}