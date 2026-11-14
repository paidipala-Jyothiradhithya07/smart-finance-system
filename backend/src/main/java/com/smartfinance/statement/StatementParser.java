package com.smartfinance.statement;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.smartfinance.config.StatementProperties;
import com.smartfinance.exception.FileProcessingException;
import com.smartfinance.statement.mapping.ColumnMapper;
import com.smartfinance.statement.mapping.ColumnMapping;
import com.smartfinance.statement.mapping.ColumnRole;
import com.smartfinance.statement.mapping.HeaderMatch;
import com.smartfinance.statement.model.ParsedStatement;
import com.smartfinance.statement.model.ParsedTransaction;
import com.smartfinance.statement.model.RowError;
import com.smartfinance.statement.normalize.DateOrder;
import com.smartfinance.statement.normalize.DateParser;
import com.smartfinance.statement.reader.RawRow;
import com.smartfinance.statement.reader.RawTable;
import com.smartfinance.statement.reader.StatementReader;

@Component
public class StatementParser {
	private static final Logger log = LoggerFactory.getLogger(StatementParser.class);

    private final List<StatementReader> readers;
    private final ColumnMapper columnMapper;
    private final StatementProperties properties;
    private final Clock clock;

    public StatementParser(List<StatementReader> readers, ColumnMapper columnMapper,
                           StatementProperties properties, Clock clock) {
        this.readers = readers;
        this.columnMapper = columnMapper;
        this.properties = properties;
        this.clock = clock;
    }

    public ParsedStatement parse(String fileName, FileType type, byte[] content) {
        StatementReader reader = readers.stream()
                .filter(candidate -> candidate.supports(type))
                .findFirst()
                .orElseThrow(() -> new FileProcessingException("This file type cannot be read yet"));

        RawTable table = reader.read(content);
        if (table.rows().isEmpty()) {
            throw new FileProcessingException("The file contains no data");
        }

        HeaderMatch header = columnMapper.findHeader(table);
        ColumnMapping mapping = header.mapping();

        List<RawRow> dataRows = table.rows().subList(header.rowIndex() + 1, table.rows().size());
        if (dataRows.isEmpty()) {
            throw new FileProcessingException("Found the column headers but no transaction rows below them");
        }
        if (dataRows.size() > properties.getMaxRows()) {
            throw new FileProcessingException("The file has more than " + properties.getMaxRows()
                    + " rows. Please upload one month at a time.");
        }

        // The day/month order is decided once, from every date in the file, never row by row
        int dateColumn = mapping.index(ColumnRole.DATE);
        List<String> dateSamples = dataRows.stream()
                .map(row -> row.cell(dateColumn))
                .filter(value -> !value.isBlank())
                .toList();
        DateOrder order = DateParser.detectOrder(dateSamples);
        if (order == DateOrder.CONFLICT) {
            throw new FileProcessingException("The date column mixes day/month/year and month/day/year formats, "
                    + "so the dates cannot be read reliably. Please use one format throughout.");
        }

        List<String> warnings = new ArrayList<>();
        if (order == DateOrder.AMBIGUOUS) {
            warnings.add("Dates such as 03/04/2026 can be read two ways. They were read as day/month/year "
                    + "(DD/MM/YYYY). Check the date range of the result.");
        }

        RowNormalizer normalizer = new RowNormalizer(mapping, new DateParser(order), LocalDate.now(clock));

        List<ParsedTransaction> transactions = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();
        int invalidRows = 0;
        int skippedRows = 0;

        for (RawRow row : dataRows) {
            if (isRepeatedHeader(row, header)) {
                skippedRows++;
                continue;
            }
            NormalizedRow result = normalizer.normalize(row);
            switch (result.status()) {
                case VALID -> transactions.add(result.transaction());
                case INVALID -> {
                    invalidRows++;
                    errors.addAll(result.errors());
                }
                case SKIPPED -> skippedRows++;
            }
        }

        // Counts only. Statement contents are private and never written to the log.
        log.info("Parsed {} statement: dataRows={}, valid={}, invalid={}, skipped={}",
                type, dataRows.size(), transactions.size(), invalidRows, skippedRows);

        return new ParsedStatement(fileName, type, header.headerRow().rowNumber(), mapping, order,
                dataRows.size(), skippedRows, invalidRows, transactions, errors, warnings);
    }

    // Some banks repeat the header at the top of every page
    private static boolean isRepeatedHeader(RawRow row, HeaderMatch header) {
        for (int index : header.mapping().mappedIndexes()) {
            String candidate = ColumnMapper.normalize(row.cell(index));
            String expected = ColumnMapper.normalize(header.headerRow().cell(index));
            if (!candidate.equals(expected)) {
                return false;
            }
        }
        return true;
    }
}
