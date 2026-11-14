package com.smartfinance.statement.reader;


import java.util.List;

public record RawRow(int rowNumber, List<String> cells) {

    public String cell(int index) {
        if (index < 0 || index >= cells.size()) {
            return "";
        }
        String value = cells.get(index);
        return value == null ? "" : value.trim();
    }

    public boolean isBlank() {
        return cells.stream().allMatch(value -> value == null || value.isBlank());
    }
}
