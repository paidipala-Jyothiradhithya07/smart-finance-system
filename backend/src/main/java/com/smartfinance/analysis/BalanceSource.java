package com.smartfinance.analysis;

public enum BalanceSource {
    STATEMENT,         // taken from the bank's own running balance
    CARRIED_FORWARD,   // the previous month's closing balance (nothing happened in between)
    CALCULATED,        // opening balance + income - expenses
    UNAVAILABLE        // the data has no balance information, so it is not guessed
}