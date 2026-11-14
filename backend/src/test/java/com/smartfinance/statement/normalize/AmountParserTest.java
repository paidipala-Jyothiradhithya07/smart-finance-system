package com.smartfinance.statement.normalize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.smartfinance.statement.normalize.AmountParser.Hint;
import com.smartfinance.statement.normalize.AmountParser.ParsedAmount;

 class AmountParserTest {
	private static ParsedAmount parse(String text) {
        return AmountParser.parse(text).orElseThrow();
    }

    @Test
    void parsesPlainAndFormattedNumbers() {
        assertThat(parse("1,200.50").value()).isEqualByComparingTo("1200.50");
        assertThat(parse("850").value()).isEqualByComparingTo("850");
        assertThat(parse("\u20B9 1,23,456.78").value()).isEqualByComparingTo("123456.78");
        assertThat(parse("Rs. 1,200").value()).isEqualByComparingTo("1200");
        assertThat(parse("INR 99.5").value()).isEqualByComparingTo("99.5");
    }

    @Test
    void understandsNegativeNotations() {
        assertThat(parse("(500)").value()).isEqualByComparingTo("-500");
        assertThat(parse("-500.00").value()).isEqualByComparingTo("-500");
        assertThat(parse("500-").value()).isEqualByComparingTo("-500");
        assertThat(parse("+500").value()).isEqualByComparingTo("500");
    }

    @Test
    void readsDebitAndCreditMarkers() {
        assertThat(parse("500 Dr").hint()).isEqualTo(Hint.DEBIT);
        assertThat(parse("500 Dr").value()).isEqualByComparingTo("500");
        assertThat(parse("1,200.00Cr").hint()).isEqualTo(Hint.CREDIT);
        assertThat(parse("Cr 750").hint()).isEqualTo(Hint.CREDIT);
        assertThat(parse("500").hint()).isNull();
    }

    @Test
    void treatsBlankCellsAndPlaceholdersAsEmpty() {
        assertThat(AmountParser.parse("")).isEmpty();
        assertThat(AmountParser.parse("   ")).isEmpty();
        assertThat(AmountParser.parse("-")).isEmpty();
        assertThat(AmountParser.parse("N/A")).isEmpty();
        assertThat(AmountParser.isBlank(null)).isTrue();
        assertThat(AmountParser.isBlank("0")).isFalse();
    }

    @Test
    void rejectsTextThatIsNotAnAmount() {
        assertThatThrownBy(() -> AmountParser.parse("abc")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AmountParser.parse("12.3.4")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AmountParser.parse("1e5")).isInstanceOf(IllegalArgumentException.class);
    }
}
