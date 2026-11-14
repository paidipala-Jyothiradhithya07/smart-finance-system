package com.smartfinance.statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.smartfinance.config.StatementProperties;
import com.smartfinance.entity.enums.TransactionType;
import com.smartfinance.exception.FileProcessingException;
import com.smartfinance.statement.mapping.ColumnMapper;
import com.smartfinance.statement.model.ParsedStatement;
import com.smartfinance.statement.model.ParsedTransaction;
import com.smartfinance.statement.model.RowError;
import com.smartfinance.statement.reader.CsvStatementReader;

public class StatementParserTest {
	private final StatementParser parser = newParser();

    private static StatementParser newParser() {
        StatementProperties properties = new StatementProperties();
        return new StatementParser(
                List.of(new CsvStatementReader(properties)),
                new ColumnMapper(properties),
                properties,
                Clock.fixed(Instant.parse("2026-11-11T00:00:00Z"), ZoneOffset.UTC));
    }

    private ParsedStatement parseCsv(String csv) {
        return parser.parse("test.csv", FileType.CSV, csv.getBytes(StandardCharsets.UTF_8));
    }

    private static String errorFor(ParsedStatement result, int row) {
        return result.errors().stream()
                .filter(error -> error.rowNumber() == row)
                .findFirst().orElseThrow().message();
    }

    @Test
    void readsTheLayoutFromTheSpecification() {
        ParsedStatement result = parseCsv("""
                Date,Description,Debit,Credit,Balance
                2026-01-01,SALARY CREDIT,,50000,70000
                2026-01-03,RENT PAYMENT,15000,,55000
                2026-01-05,SWIGGY,850,,54150
                2026-01-10,UBER,500,,53650
                2026-01-15,AMAZON,2500,,51150
                """);

        assertThat(result.headerRowNumber()).isEqualTo(1);
        assertThat(result.transactions()).hasSize(5);
        assertThat(result.errors()).isEmpty();
        assertThat(result.warnings()).isEmpty();

        ParsedTransaction salary = result.transactions().get(0);
        assertThat(salary.date()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(salary.description()).isEqualTo("SALARY CREDIT");
        assertThat(salary.type()).isEqualTo(TransactionType.CREDIT);
        assertThat(salary.amount()).isEqualByComparingTo("50000.00");
        assertThat(salary.balance()).isEqualByComparingTo("70000");

        ParsedTransaction swiggy = result.transactions().get(2);
        assertThat(swiggy.type()).isEqualTo(TransactionType.DEBIT);
        assertThat(swiggy.amount()).isEqualByComparingTo("850");

        assertThat(result.periodStart()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(result.periodEnd()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(result.monthCounts()).containsExactly(entry(YearMonth.of(2026, 1), 5L));
    }

    @Test
    void findsTheHeaderBelowABankHeadingBlockAndUnderstandsBankColumnNames() {
        ParsedStatement result = parseCsv("""
                HDFC BANK LTD
                Account Statement
                Account No: 50100123456789,,,,,
                ,,,,,,
                Date,Narration,Chq./Ref.No.,Value Dt,Withdrawal Amt.,Deposit Amt.,Closing Balance
                01/02/26,SALARY CREDIT,REF001,01/02/26,,"55,000.00","93,000.00"
                03/02/26,RENT PAYMENT,REF002,03/02/26,"15,000.00",,"78,000.00"
                """);

        assertThat(result.headerRowNumber()).isEqualTo(5);
        assertThat(result.transactions()).hasSize(2);
        assertThat(result.mapping().describe())
                .containsEntry("description", "Narration")
                .containsEntry("debit", "Withdrawal Amt.")
                .containsEntry("credit", "Deposit Amt.")
                .containsEntry("balance", "Closing Balance")
                .containsEntry("reference", "Chq./Ref.No.");

        ParsedTransaction salary = result.transactions().get(0);
        assertThat(salary.rowNumber()).isEqualTo(6);
        assertThat(salary.date()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(salary.amount()).isEqualByComparingTo("55000");
        assertThat(salary.reference()).isEqualTo("REF001");

        // 01/02 and 03/02 cannot prove the order, so the file is read day-first and the user is warned
        assertThat(result.warnings()).hasSize(1);
        assertThat(result.warnings().get(0)).contains("day/month/year");
    }

    @Test
    void usesTheSignOfASingleAmountColumn() {
        ParsedStatement result = parseCsv("""
                Txn Date,Description,Amount,Balance
                01-Mar-2026,SALARY CREDIT,50000.00,108000.00
                02-Mar-2026,RENT PAYMENT,-15000.00,93000.00
                04-Mar-2026,SWIGGY,-880.00,92120.00
                """);

        assertThat(result.errors()).isEmpty();
        assertThat(result.transactions()).extracting(ParsedTransaction::type)
                .containsExactly(TransactionType.CREDIT, TransactionType.DEBIT, TransactionType.DEBIT);
        assertThat(result.transactions().get(1).amount()).isEqualByComparingTo("15000");
    }

    @Test
    void usesATypeColumnWhenThereIsOne() {
        ParsedStatement result = parseCsv("""
                Date,Narration,Amount,Dr/Cr,Balance
                05/03/2026,SALARY,"50,000.00",CR,"108,000.00"
                13/03/2026,AMAZON,"2,251.00",DR,"83,749.00"
                """);

        assertThat(result.errors()).isEmpty();
        assertThat(result.transactions().get(0).type()).isEqualTo(TransactionType.CREDIT);
        assertThat(result.transactions().get(1).type()).isEqualTo(TransactionType.DEBIT);
        assertThat(result.warnings()).isEmpty();           // 13/03 proves day-first
    }

    @Test
    void reportsInvalidRowsAndSkipsNonTransactionLines() {
        ParsedStatement result = parseCsv("""
                Date,Description,Debit,Credit,Balance
                2026-04-01,SALARY CREDIT,,50000,70000
                2026-04-02,RENT PAYMENT,15000,,55000
                31/02/2026,SWIGGY,850,,54150
                2026-04-05,,500,,53650
                2026-04-06,UBER,500,200,53350
                2026-04-07,AMAZON,abc,,
                2026-04-08,NETFLIX,0,,

                Date,Description,Debit,Credit,Balance
                2026-04-10,ZOMATO,780,,52570
                ,Closing Balance,,,52570
                2099-12-31,FUTURE TRANSACTION,100,,
                """);

        assertThat(result.totalDataRows()).isEqualTo(11);
        assertThat(result.transactions()).hasSize(3);
        assertThat(result.invalidRows()).isEqualTo(6);
        assertThat(result.skippedRows()).isEqualTo(2);        // the repeated header and the closing balance line

        assertThat(result.errors()).extracting(RowError::rowNumber).containsExactly(4, 5, 6, 7, 8, 13);
        assertThat(errorFor(result, 4)).contains("not a valid date");
        assertThat(errorFor(result, 5)).contains("Description is missing");
        assertThat(errorFor(result, 6)).contains("Both debit and credit");
        assertThat(errorFor(result, 7)).contains("not a valid amount");
        assertThat(errorFor(result, 8)).contains("greater than zero");
        assertThat(errorFor(result, 13)).contains("in the future");

        assertThat(result.monthCounts()).containsExactly(entry(YearMonth.of(2026, 4), 3L));
    }

    @Test
    void failsClearlyWhenThereAreNoColumnHeaders() {
        assertThatThrownBy(() -> parseCsv("foo,bar\n1,2\n"))
                .isInstanceOf(FileProcessingException.class)
                .hasMessageContaining("column headers");
    }

    @Test
    void rejectsAFileThatMixesDayFirstAndMonthFirstDates() {
        assertThatThrownBy(() -> parseCsv("""
                Date,Description,Debit,Credit,Balance
                13/01/2026,A,10,,
                01/13/2026,B,10,,
                """))
                .isInstanceOf(FileProcessingException.class)
                .hasMessageContaining("mixes");
    }
}
