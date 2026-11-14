package com.smartfinance.statement.reader;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import com.smartfinance.config.StatementProperties;
import com.smartfinance.exception.FileProcessingException;
import com.smartfinance.statement.FileType;

@Component
public class CsvStatementReader implements StatementReader {

    private static final char[] DELIMITERS = {',', ';', '\t', '|'};
    private static final int LINES_TO_INSPECT = 30;

    private final StatementProperties properties;

    public CsvStatementReader(StatementProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean supports(FileType type) {
        return type == FileType.CSV;
    }

    @Override
    public RawTable read(byte[] content) {
        String text = decode(content);
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);                       // remove the UTF-8 byte order mark
        }
        char delimiter = detectDelimiter(text);

        // Blank lines are kept while reading, so row numbers match the line numbers in the file
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setQuote('"')
                .setIgnoreEmptyLines(false)
                .setTrim(true)
                .build();

        int limit = properties.getMaxRows() + properties.getHeaderScanRows();
        List<RawRow> rows = new ArrayList<>();
        try (CSVParser parser = format.parse(new StringReader(text))) {
            for (CSVRecord record : parser) {
                RawRow row = new RawRow((int) record.getRecordNumber(), record.toList());
                if (row.isBlank()) {
                    continue;
                }
                if (rows.size() >= limit) {
                    throw new FileProcessingException("The file has more than " + properties.getMaxRows()
                            + " rows. Please upload one month at a time.");
                }
                rows.add(row);
            }
        } catch (FileProcessingException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new FileProcessingException("The CSV file could not be read. Check for an unclosed quote (\") "
                    + "or a damaged line.");
        }
        return new RawTable(rows);
    }

    // UTF-8 first. If the bytes are not valid UTF-8 (older Windows exports), fall back to Windows-1252.
    private static String decode(byte[] content) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
        } catch (CharacterCodingException ex) {
            return new String(content, Charset.forName("windows-1252"));
        }
    }

    // Counts each candidate delimiter outside quotes over the first lines. The most frequent one wins.
    private static char detectDelimiter(String text) {
        int[] totals = new int[DELIMITERS.length];
        int inspected = 0;
        for (String line : text.split("\\R", LINES_TO_INSPECT * 2)) {
            if (line.isBlank()) {
                continue;
            }
            for (int i = 0; i < DELIMITERS.length; i++) {
                totals[i] += countOutsideQuotes(line, DELIMITERS[i]);
            }
            if (++inspected >= LINES_TO_INSPECT) {
                break;
            }
        }
        int best = 0;
        for (int i = 1; i < totals.length; i++) {
            if (totals[i] > totals[best]) {
                best = i;
            }
        }
        return totals[best] == 0 ? ',' : DELIMITERS[best];
    }

    private static int countOutsideQuotes(String line, char delimiter) {
        boolean inQuotes = false;
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == delimiter && !inQuotes) {
                count++;
            }
        }
        return count;
    }
}
