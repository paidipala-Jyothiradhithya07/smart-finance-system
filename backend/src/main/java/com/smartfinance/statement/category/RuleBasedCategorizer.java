package com.smartfinance.statement.category;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.smartfinance.config.CategoryRuleProperties;
import com.smartfinance.entity.enums.TransactionType;
import com.smartfinance.statement.TextNormalizer;

@Component
public class RuleBasedCategorizer implements TransactionCategorizer {

    private record Rule(String category, String paddedKeyword, int length) {
    }

    private final List<Rule> debitRules;
    private final List<Rule> creditRules;

    public RuleBasedCategorizer(CategoryRuleProperties properties) {
        this.debitRules = buildRules(properties.getDebit());
        this.creditRules = buildRules(properties.getCredit());
    }

    /**
     * Whole words only: "rent" matches "RENT PAYMENT" but not "CURRENT ACCOUNT", and "ola" does not match "COLA".
     * When several keywords match, the longest wins, so "AMAZON PRIME VIDEO" becomes Entertainment, not Shopping.
     */
    @Override
    public String suggestCategoryName(String description, TransactionType type) {
        String text = " " + TextNormalizer.normalize(description) + " ";
        List<Rule> rules = type == TransactionType.CREDIT ? creditRules : debitRules;

        Rule best = null;
        for (Rule rule : rules) {
            if (text.contains(rule.paddedKeyword()) && (best == null || rule.length() > best.length())) {
                best = rule;
            }
        }
        return best == null ? FALLBACK_CATEGORY : best.category();
    }

    private static List<Rule> buildRules(Map<String, List<String>> source) {
        List<Rule> rules = new ArrayList<>();
        source.forEach((category, keywords) -> {
            for (String keyword : keywords) {
                String normalized = TextNormalizer.normalize(keyword);
                if (!normalized.isEmpty()) {
                    rules.add(new Rule(category, " " + normalized + " ", normalized.length()));
                }
            }
        });
        return List.copyOf(rules);
    }
}