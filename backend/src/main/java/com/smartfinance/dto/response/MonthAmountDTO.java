package com.smartfinance.dto.response;

import java.math.BigDecimal;

public record MonthAmountDTO(String month, String label, BigDecimal amount) {
}