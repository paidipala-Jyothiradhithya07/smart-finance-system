package com.smartfinance.statement.mapping;

import com.smartfinance.statement.reader.RawRow;

public record HeaderMatch(int rowIndex, RawRow headerRow, ColumnMapping mapping){

}
