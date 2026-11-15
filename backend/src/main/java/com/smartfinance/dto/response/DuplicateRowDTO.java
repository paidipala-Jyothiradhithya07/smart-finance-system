package com.smartfinance.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.smartfinance.entity.enums.TransactionType;

public record DuplicateRowDTO(int row,
		LocalDate date,
		String description,
		BigDecimal amount,
		TransactionType type) {
}
