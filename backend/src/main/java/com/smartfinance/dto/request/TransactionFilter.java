package com.smartfinance.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.smartfinance.entity.enums.TransactionSource;
import com.smartfinance.entity.enums.TransactionType;

public record TransactionFilter(
        LocalDate from,
        LocalDate to,
        Long categoryId,
        TransactionType type,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String keyword,
        TransactionSource source) {
}
