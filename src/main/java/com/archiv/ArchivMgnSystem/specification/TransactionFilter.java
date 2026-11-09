package com.archiv.ArchivMgnSystem.specification;

import com.archiv.ArchivMgnSystem.models.Transaction;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TransactionFilter {

    public static Specification<Transaction> byFilter(String searchValue) {

        return ((root, query, criteriaBuilder) -> {
            if (searchValue == null || searchValue.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            String searchPattern = "%" + searchValue.toLowerCase() + "%";

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("description"), "")), searchPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("note"), "")), searchPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("transactionStatus").as(String.class)), searchPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("transactionType").as(String.class)), searchPattern));

            Join<Object, Object> userJoin = root.join("user", JoinType.LEFT);
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("username")), searchPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("email")), searchPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("phoneNumber")), searchPattern));

            Join<Object, Object> itemJoin = root.join("item", JoinType.LEFT);
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(itemJoin.get("name")), searchPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(itemJoin.get("sku")), searchPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(itemJoin.get("description"), "")), searchPattern));

            Join<Object, Object> categoryJoin = itemJoin.join("category", JoinType.LEFT);
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(categoryJoin.get("name")), searchPattern));

            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        });

    }

    public static Specification<Transaction> byMonthAndYear(int month, int year) {
        return (root, query, criteriaBuilder) -> {
            Expression<Integer> monthExpression = criteriaBuilder.function("month", Integer.class, root.get("createdAt"));
            Expression<Integer> yearExpression = criteriaBuilder.function("year", Integer.class, root.get("createdAt"));

            Predicate monthPredicate = criteriaBuilder.equal(monthExpression, month);
            Predicate yearPredicate = criteriaBuilder.equal(yearExpression, year);

            return criteriaBuilder.and(monthPredicate, yearPredicate);

        };

    }
}
