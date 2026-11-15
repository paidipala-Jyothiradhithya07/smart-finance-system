package com.smartfinance.dto.response;

public enum ImportStatus {
    IMPORTED,               // everything valid was imported
    IMPORTED_WITH_ERRORS,   // imported, but some rows were invalid and left out
    NO_NEW_TRANSACTIONS,    // every valid row was already in the database
    ALREADY_IMPORTED,       // this exact file was imported before
    FAILED                  // nothing was imported
}