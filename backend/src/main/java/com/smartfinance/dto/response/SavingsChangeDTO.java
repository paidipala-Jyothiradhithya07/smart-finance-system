package com.smartfinance.dto.response;

import java.math.BigDecimal;

public record SavingsChangeDTO(String month, String label, BigDecimal savings,
                               BigDecimal changeFromPrevious, BigDecimal changePercent) {
}