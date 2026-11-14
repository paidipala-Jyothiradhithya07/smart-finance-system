package com.smartfinance.statement.mapping;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.smartfinance.config.StatementProperties;
import com.smartfinance.exception.FileProcessingException;
import com.smartfinance.statement.reader.RawRow;
import com.smartfinance.statement.reader.RawTable;

@Component
public class ColumnMapper {
	private final int headerScanRows;
    private final EnumMap<ColumnRole, List<String>> aliases = new EnumMap<>(ColumnRole.class);

    public ColumnMapper(StatementProperties properties) {
        this.headerScanRows = properties.getHeaderScanRows();
        for (ColumnRole role : ColumnRole.values()) {
            List<String> configured = properties.getAliases()
                    .getOrDefault(role.name().toLowerCase(Locale.ROOT), List.of());
            aliases.put(role, configured.stream().map(ColumnMapper::normalize).toList());
        }
    }

    /** "Chq./Ref.No." becomes "chq ref no": lower case, everything except letters and digits becomes one space. */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** Looks through the first rows for the one that holds the column headers. */
    public HeaderMatch findHeader(RawTable table) {
        List<RawRow> rows = table.rows();
        int limit = Math.min(rows.size(), headerScanRows);

        ColumnMapping closest = null;
        RawRow closestRow = null;
        for (int i = 0; i < limit; i++) {
            RawRow row = rows.get(i);
            ColumnMapping mapping = mapRow(row.cells());
            if (mapping.isUsable()) {
                return new HeaderMatch(i, row, mapping);
            }
            if (closest == null || mapping.size() > closest.size()) {
                closest = mapping;
                closestRow = row;
            }
        }
        throw new FileProcessingException(notFoundMessage(closest, closestRow));
    }

    public ColumnMapping mapRow(List<String> cells) {
        List<String> normalized = cells.stream().map(ColumnMapper::normalize).toList();
        EnumMap<ColumnRole, Integer> indexes = new EnumMap<>(ColumnRole.class);
        Set<Integer> used = new HashSet<>();

        // Pass 1: exact header names. Pass 2: headers that start with a known name ("withdrawal amt").
        for (boolean exact : new boolean[] {true, false}) {
            for (ColumnRole role : ColumnRole.values()) {
                if (indexes.containsKey(role)) {
                    continue;
                }
                int bestColumn = -1;
                int bestRank = Integer.MAX_VALUE;
                for (int column = 0; column < normalized.size(); column++) {
                    if (used.contains(column) || normalized.get(column).isEmpty()) {
                        continue;
                    }
                    int rank = rank(role, normalized.get(column), exact);
                    if (rank >= 0 && rank < bestRank) {
                        bestRank = rank;
                        bestColumn = column;
                    }
                }
                if (bestColumn >= 0) {
                    indexes.put(role, bestColumn);
                    used.add(bestColumn);
                }
            }
        }

        EnumMap<ColumnRole, String> headers = new EnumMap<>(ColumnRole.class);
        indexes.forEach((role, column) -> headers.put(role, cells.get(column).trim()));
        return new ColumnMapping(indexes, headers);
    }

    // The position of the alias in the list. Lower is preferred ("transaction date" beats "value date").
    private int rank(ColumnRole role, String text, boolean exact) {
        List<String> list = aliases.get(role);
        for (int i = 0; i < list.size(); i++) {
            String alias = list.get(i);
            if (exact ? text.equals(alias) : text.startsWith(alias + " ")) {
                return i;
            }
        }
        return -1;
    }

    private String notFoundMessage(ColumnMapping closest, RawRow closestRow) {
        StringBuilder message = new StringBuilder(
                "Could not find the column headers. A statement needs a date column, a description column "
                + "(Description, Narration or Particulars) and either Debit/Credit columns or an Amount column.");
        if (closest != null && closest.size() > 0) {
            message.append(" The closest match was row ").append(closestRow.rowNumber())
                    .append(", where only these columns were recognised: ")
                    .append(String.join(", ", closest.describe().keySet())).append('.');
        }
        return message.toString();
    }

	
}
