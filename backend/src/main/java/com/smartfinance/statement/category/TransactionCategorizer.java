package com.smartfinance.statement.category;

import com.smartfinance.entity.enums.TransactionType;

public interface TransactionCategorizer {

    /** Used when nothing matches. This is a system category that every user has. */
    String FALLBACK_CATEGORY = "Other";

    /** Returns the name of a system category. Never null: when nothing matches, the fallback is returned. */
    String suggestCategoryName(String description, TransactionType type);
}