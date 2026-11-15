package com.smartfinance.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Keywords per category. Debit rules only point to expense categories and credit rules only to income
 * categories. The defaults live here. They can be overridden under app.categorization in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "app.categorization")
public class CategoryRuleProperties {

    private Map<String, List<String>> debit = defaultDebitRules();
    private Map<String, List<String>> credit = defaultCreditRules();

    public Map<String, List<String>> getDebit() { return debit; }
    public void setDebit(Map<String, List<String>> debit) { this.debit = debit; }

    public Map<String, List<String>> getCredit() { return credit; }
    public void setCredit(Map<String, List<String>> credit) { this.credit = credit; }

    public static Map<String, List<String>> defaultDebitRules() {
        Map<String, List<String>> rules = new LinkedHashMap<>();
        rules.put("Food", List.of("swiggy", "zomato", "bigbasket", "blinkit", "zepto", "instamart",
                "dominos", "mcdonalds", "kfc", "starbucks", "restaurant", "cafe", "chai",
                "grocery", "groceries", "supermarket", "dmart"));
        rules.put("Shopping", List.of("amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa",
                "decathlon", "mall", "shopping"));
        rules.put("Transportation", List.of("uber", "ola", "rapido", "petrol", "diesel", "fuel", "metro",
                "fastag", "parking", "bpcl", "hpcl", "iocl", "indian oil"));
        rules.put("Rent", List.of("rent", "rent payment", "house rent", "landlord"));
        rules.put("Bills", List.of("electricity", "water bill", "gas bill", "lpg", "broadband", "recharge",
                "airtel", "jio", "bsnl", "bill payment", "bill"));
        rules.put("Entertainment", List.of("netflix", "spotify", "hotstar", "prime video", "amazon prime",
                "youtube premium", "bookmyshow", "pvr", "inox", "zee5", "sonyliv"));
        rules.put("Healthcare", List.of("pharmacy", "apollo", "medplus", "hospital", "clinic", "doctor",
                "diagnostic", "diagnostics", "pharmeasy", "netmeds", "1mg", "dental"));
        rules.put("Education", List.of("udemy", "coursera", "unacademy", "byjus", "tuition",
                "school fees", "college fees", "course"));
        rules.put("Travel", List.of("makemytrip", "goibibo", "irctc", "cleartrip", "redbus", "indigo",
                "air india", "vistara", "airline", "hotel", "oyo", "airbnb", "travel"));
        rules.put("EMI", List.of("emi", "loan"));
        return rules;
    }

    public static Map<String, List<String>> defaultCreditRules() {
        Map<String, List<String>> rules = new LinkedHashMap<>();
        rules.put("Salary", List.of("salary", "payroll", "sal credit"));
        rules.put("Freelancing", List.of("freelance", "freelancing", "upwork", "fiverr", "consulting"));
        rules.put("Business", List.of("business", "invoice", "client payment"));
        rules.put("Interest", List.of("interest", "int pd", "int cr"));
        rules.put("Bonus", List.of("bonus", "incentive"));
        return rules;
    }
}