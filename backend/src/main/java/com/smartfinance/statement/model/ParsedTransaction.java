package com.smartfinance.statement.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.smartfinance.entity.enums.TransactionType;

public record ParsedTransaction(
        int rowNumber,
        LocalDate date,
        String description,
        BigDecimal amount,
        TransactionType type,
        BigDecimal balance,
        String reference){
}
