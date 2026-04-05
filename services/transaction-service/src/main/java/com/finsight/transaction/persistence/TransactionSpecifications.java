package com.finsight.transaction.persistence;

import com.finsight.transaction.domain.Transaction;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public class TransactionSpecifications {

    public static Specification<Transaction> ownedBy(String ownerEmail) {
        return (root, query, cb) -> cb.equal(root.get("ownerEmail"), ownerEmail);
    }

    public static Specification<Transaction> ofType(String type) {
        if (type == null || type.isBlank()) return null;
        return (root, query, cb) -> cb.equal(cb.upper(root.get("type")), type.toUpperCase());
    }

    public static Specification<Transaction> afterDate(Instant from) {
        if (from == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), from);
    }

    public static Specification<Transaction> beforeDate(Instant to) {
        if (to == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), to);
    }

    public static Specification<Transaction> inCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) return null;
        return (root, query, cb) ->
                cb.equal(cb.upper(root.join("category").get("name")), categoryName.toUpperCase());
    }

    public static Specification<Transaction> keywordSearch(String search) {
        if (search == null || search.isBlank()) return null;
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("rawDescription")), pattern),
                cb.like(cb.lower(root.get("normalizedMerchant")), pattern)
        );
    }
}
