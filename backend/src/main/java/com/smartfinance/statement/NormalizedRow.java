package com.smartfinance.statement;


import java.util.List;

import com.smartfinance.statement.model.ParsedTransaction;
import com.smartfinance.statement.model.RowError;

public  record NormalizedRow(Status status, ParsedTransaction transaction, List<RowError> errors) {

    public enum Status { VALID, INVALID, SKIPPED }

    public static NormalizedRow valid(ParsedTransaction transaction) {
        return new NormalizedRow(Status.VALID, transaction, List.of());
    }

    public static NormalizedRow invalid(List<RowError> errors) {
        return new NormalizedRow(Status.INVALID, null, errors);
    }

    public static NormalizedRow skipped() {
        return new NormalizedRow(Status.SKIPPED, null, List.of());
    }
}
