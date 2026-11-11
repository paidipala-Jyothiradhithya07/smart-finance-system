package com.smartfinance.repository.spec;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.smartfinance.dto.request.TransactionFilter;
import com.smartfinance.entity.Transaction;

import jakarta.persistence.criteria.Predicate;

public final class TransactionSpecifications {

    // "!" is the escape character in LIKE patterns. It avoids backslash problems in MySQL.
    private static final char ESCAPE = '!';

    private TransactionSpecifications() {
    }

    /** Builds the WHERE clause. The user condition is always added first. */
    public static Specification<Transaction> forUser(Long userId, TransactionFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Nobody can ever see another user's rows, whatever the other filters say
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (filter.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("transactionDate"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDate>get("transactionDate"), filter.to()));
            }
            if (filter.categoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), filter.categoryId()));
            }
            if (filter.type() != null) {
                predicates.add(cb.equal(root.get("type"), filter.type()));
            }
            if (filter.source() != null) {
                predicates.add(cb.equal(root.get("source"), filter.source()));
            }
            if (filter.minAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("amount"), filter.minAmount()));
            }
            if (filter.maxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("amount"), filter.maxAmount()));
            }
            if (filter.keyword() != null && !filter.keyword().isBlank()) {
                String pattern = "%" + escapeLike(filter.keyword().trim().toLowerCase(Locale.ROOT)) + "%";
                Predicate inDescription = cb.like(cb.lower(root.<String>get("description")), pattern, ESCAPE);
                Predicate inReference = cb.like(cb.lower(root.<String>get("referenceNo")), pattern, ESCAPE);
                predicates.add(cb.or(inDescription, inReference));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Makes "%" and "_" typed by the user match themselves instead of acting as wildcards. */
    public static String escapeLike(String value) {
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
