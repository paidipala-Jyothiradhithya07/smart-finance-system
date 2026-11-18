package com.smartfinance.dto.response;

import java.math.BigDecimal;

public record CategoryAmountDTO(Long categoryId, String category, BigDecimal amount,
                                BigDecimal percentage, long transactionCount) {
}