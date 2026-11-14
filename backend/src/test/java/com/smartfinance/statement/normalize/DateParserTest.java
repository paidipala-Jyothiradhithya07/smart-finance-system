package com.smartfinance.statement.normalize;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

 class DateParserTest {
	  private final DateParser dayFirst = new DateParser(DateOrder.DAY_FIRST);

	    @Test
	    void parsesIsoDates() {
	        assertThat(dayFirst.parse("2026-01-05")).contains(LocalDate.of(2026, 1, 5));
	    }

	    @Test
	    void parsesNumericDatesDayFirst() {
	        assertThat(dayFirst.parse("05/01/2026")).contains(LocalDate.of(2026, 1, 5));
	        assertThat(dayFirst.parse("05-01-2026")).contains(LocalDate.of(2026, 1, 5));
	        assertThat(dayFirst.parse("05.01.2026")).contains(LocalDate.of(2026, 1, 5));
	        assertThat(dayFirst.parse("05/01/26")).contains(LocalDate.of(2026, 1, 5));
	    }

	    @Test
	    void parsesMonthNames() {
	        assertThat(dayFirst.parse("05-Jan-2026")).contains(LocalDate.of(2026, 1, 5));
	        assertThat(dayFirst.parse("05 January 2026")).contains(LocalDate.of(2026, 1, 5));
	        assertThat(dayFirst.parse("Jan 05, 2026")).contains(LocalDate.of(2026, 1, 5));
	        assertThat(dayFirst.parse("5-Sep-26")).contains(LocalDate.of(2026, 9, 5));
	    }

	    @Test
	    void ignoresTheTimePart() {
	        assertThat(dayFirst.parse("2026-01-05 10:30:00")).contains(LocalDate.of(2026, 1, 5));
	        assertThat(dayFirst.parse("2026-01-05T10:30:00")).contains(LocalDate.of(2026, 1, 5));
	        assertThat(dayFirst.parse("05/01/2026 10:30")).contains(LocalDate.of(2026, 1, 5));
	    }

	    @Test
	    void monthFirstParserReadsTheOtherOrder() {
	        DateParser monthFirst = new DateParser(DateOrder.MONTH_FIRST);
	        assertThat(monthFirst.parse("01/13/2026")).contains(LocalDate.of(2026, 1, 13));
	    }

	    @Test
	    void rejectsInvalidDates() {
	        assertThat(dayFirst.parse("31/02/2026")).isEmpty();
	        assertThat(dayFirst.parse("13/13/2026")).isEmpty();
	        assertThat(dayFirst.parse("2026-13-01")).isEmpty();
	        assertThat(dayFirst.parse("abc")).isEmpty();
	        assertThat(dayFirst.parse("5/1")).isEmpty();
	        assertThat(dayFirst.parse("")).isEmpty();
	        assertThat(dayFirst.parse(null)).isEmpty();
	    }

	    @Test
	    void detectsDayFirstWhenADayIsAbove12() {
	        assertThat(DateParser.detectOrder(List.of("03/04/2026", "13/04/2026"))).isEqualTo(DateOrder.DAY_FIRST);
	    }

	    @Test
	    void detectsMonthFirstWhenTheSecondNumberIsAbove12() {
	        assertThat(DateParser.detectOrder(List.of("03/04/2026", "04/13/2026"))).isEqualTo(DateOrder.MONTH_FIRST);
	    }

	    @Test
	    void reportsAmbiguousWhenNothingSettlesIt() {
	        assertThat(DateParser.detectOrder(List.of("03/04/2026", "05/06/2026"))).isEqualTo(DateOrder.AMBIGUOUS);
	    }

	    @Test
	    void reportsConflictWhenBothOrdersAppear() {
	        assertThat(DateParser.detectOrder(List.of("13/01/2026", "01/13/2026"))).isEqualTo(DateOrder.CONFLICT);
	    }

	    @Test
	    void isoDatesAreNeverAmbiguous() {
	        assertThat(DateParser.detectOrder(List.of("2026-01-05", "2026-02-03"))).isEqualTo(DateOrder.DAY_FIRST);
	    }
}
