package com.smartfinance.statement.mapping;


import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ColumnMapping {
	private final EnumMap<ColumnRole, Integer> indexes;
    private final EnumMap<ColumnRole, String> headers;

    public ColumnMapping(EnumMap<ColumnRole, Integer> indexes, EnumMap<ColumnRole, String> headers) {
        this.indexes = indexes;
        this.headers = headers;
    }

    public boolean has(ColumnRole role) {
        return indexes.containsKey(role);
    }

    public int index(ColumnRole role) {
        return indexes.get(role);
    }

    public String header(ColumnRole role) {
        return headers.getOrDefault(role, role.name().toLowerCase(Locale.ROOT));
    }

    public boolean hasAmountSource() {
        return has(ColumnRole.DEBIT) || has(ColumnRole.CREDIT) || has(ColumnRole.AMOUNT);
    }

    /** A usable header needs a date, a description and some way to know the amount. */
    public boolean isUsable() {
        return has(ColumnRole.DATE) && has(ColumnRole.DESCRIPTION) && hasAmountSource();
    }

    public Set<Integer> mappedIndexes() {
        return new HashSet<>(indexes.values());
    }

    public int size() {
        return indexes.size();
    }

    /** For the API response: role name to the header text used in the file. */
    public Map<String, String> describe() {
        Map<String, String> result = new LinkedHashMap<>();
        headers.forEach((role, header) -> result.put(role.name().toLowerCase(Locale.ROOT), header));
        return result;
    }
}
