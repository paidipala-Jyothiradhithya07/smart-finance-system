package com.smartfinance.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.smartfinance.entity.enums.TransactionSource;
import com.smartfinance.entity.enums.TransactionType;

public record TransactionResponseDTO(
        Long id,
        LocalDate transactionDate,
        String description,
        BigDecimal amount,
        TransactionType type,
        Long categoryId,
        String categoryName,
        String paymentMethod,
        TransactionSource source,
        String referenceNo,
        BigDecimal balanceAfter,
        Long bankStatementId,
        LocalDateTime createdAt) {
}
