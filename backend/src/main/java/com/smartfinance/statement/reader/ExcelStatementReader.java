package com.smartfinance.statement.reader;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import com.smartfinance.config.StatementProperties;
import com.smartfinance.exception.FileProcessingException;
import com.smartfinance.statement.FileType;

@Component
public class ExcelStatementReader implements StatementReader {

    private static final int MAX_COLUMNS = 60;

    private final StatementProperties properties;

    public ExcelStatementReader(StatementProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean supports(FileType type) {
        return type == FileType.XLSX || type == FileType.XLS;
    }

    @Override
    public RawTable read(byte[] content) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            // Only the first sheet that contains data is read
            for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
                RawTable table = readSheet(workbook.getSheetAt(index));
                if (!table.rows().isEmpty()) {
                    return table;
                }
            }
            return new RawTable(List.of());
        } catch (FileProcessingException ex) {
            throw ex;
        } catch (EncryptedDocumentException ex) {
            throw new FileProcessingException("The Excel file is password-protected. "
                    + "Remove the password and upload it again.");
        } catch (IOException | RuntimeException ex) {
            throw new FileProcessingException("The Excel file could not be read. It may be damaged. "
                    + "Try opening it in Excel and saving it again as .xlsx or CSV.");
        }
    }

    private RawTable readSheet(Sheet sheet) {
        int limit = properties.getMaxRows() + properties.getHeaderScanRows();
        List<RawRow> rows = new ArrayList<>();
        for (Row row : sheet) {
            List<String> cells = new ArrayList<>();
            int lastCell = Math.min(row.getLastCellNum(), MAX_COLUMNS);
            for (int column = 0; column < lastCell; column++) {
                cells.add(cellText(row.getCell(column)));
            }
            RawRow raw = new RawRow(row.getRowNum() + 1, cells);
            if (raw.isBlank()) {
                continue;
            }
            if (rows.size() >= limit) {
                throw new FileProcessingException("The sheet has more than " + properties.getMaxRows()
                        + " rows. Please upload one month at a time.");
            }
            rows.add(raw);
        }
        return new RawTable(rows);
    }

    private static String cellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        return switch (type) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> numericText(cell);
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    // Date cells become ISO dates (no ambiguity). Other numbers are written out in full, with no scientific notation.
    private static String numericText(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            LocalDateTime dateTime = cell.getLocalDateTimeCellValue();
            return dateTime == null ? "" : dateTime.toLocalDate().toString();
        }
        return BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
    }
}
