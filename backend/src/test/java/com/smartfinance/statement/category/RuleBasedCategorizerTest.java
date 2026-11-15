package com.smartfinance.statement.category;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.smartfinance.config.CategoryRuleProperties;
import com.smartfinance.entity.enums.TransactionType;

class RuleBasedCategorizerTest {

    private final RuleBasedCategorizer categorizer = new RuleBasedCategorizer(new CategoryRuleProperties());

    private String debit(String description) {
        return categorizer.suggestCategoryName(description, TransactionType.DEBIT);
    }

    private String credit(String description) {
        return categorizer.suggestCategoryName(description, TransactionType.CREDIT);
    }

    @Test
    void commonExpensesAreRecognised() {
        assertThat(debit("SWIGGY")).isEqualTo("Food");
        assertThat(debit("UPI/ZOMATO/ORDER 123")).isEqualTo("Food");
        assertThat(debit("UBER INDIA")).isEqualTo("Transportation");
        assertThat(debit("AMAZON PAY")).isEqualTo("Shopping");
        assertThat(debit("NETFLIX.COM")).isEqualTo("Entertainment");
        assertThat(debit("APOLLO PHARMACY")).isEqualTo("Healthcare");
        assertThat(debit("RENT PAYMENT")).isEqualTo("Rent");
        assertThat(debit("ELECTRICITY BILL")).isEqualTo("Bills");
        assertThat(debit("UDEMY COURSE")).isEqualTo("Education");
        assertThat(debit("MAKEMYTRIP TRAVEL")).isEqualTo("Travel");
        assertThat(debit("HOME LOAN EMI")).isEqualTo("EMI");
    }

    @Test
    void incomeIsRecognisedOnlyForCredits() {
        assertThat(credit("SALARY CREDIT")).isEqualTo("Salary");
        assertThat(credit("INTEREST CREDIT")).isEqualTo("Interest");
        assertThat(credit("UPWORK PAYMENT")).isEqualTo("Freelancing");
        assertThat(debit("SALARY")).isEqualTo("Other");
        assertThat(credit("SWIGGY REFUND")).isEqualTo("Other");
    }

    @Test
    void onlyWholeWordsMatch() {
        assertThat(debit("CURRENT ACCOUNT FEE")).isEqualTo("Other");   // contains "rent" inside "current"
        assertThat(debit("COLA SHOP")).isEqualTo("Other");              // contains "ola" inside "cola"
    }

    @Test
    void theLongestMatchingKeywordWins() {
        // "amazon" says Shopping, but "amazon prime" and "prime video" say Entertainment
        assertThat(debit("AMAZON PRIME VIDEO")).isEqualTo("Entertainment");
    }

    @Test
    void unknownOrEmptyDescriptionsFallBackToOther() {
        assertThat(debit("XYZ TRADERS")).isEqualTo("Other");
        assertThat(debit("")).isEqualTo("Other");
        assertThat(debit(null)).isEqualTo("Other");
    }
}