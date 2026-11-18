package com.smartfinance.analysis;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.smartfinance.entity.enums.TransactionType;

/** One bank row that carries a running balance. */
public record BalancePoint(LocalDate date, TransactionType type, BigDecimal amount, BigDecimal balanceAfter) {

    /** The balance just before this row happened. */
    public BigDecimal balanceBefore() {
        return type == TransactionType.CREDIT ? balanceAfter.subtract(amount) : balanceAfter.add(amount);
    }
}