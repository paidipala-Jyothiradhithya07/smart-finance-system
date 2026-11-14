package com.smartfinance.statement.reader;

import com.smartfinance.statement.FileType;

public interface StatementReader {

    boolean supports(FileType type);

    /** Reads the file into rows of text. Completely blank rows are left out. */
    RawTable read(byte[] content);

}
