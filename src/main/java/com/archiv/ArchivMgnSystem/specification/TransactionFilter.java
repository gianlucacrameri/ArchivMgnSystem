package com.archiv.ArchivMgnSystem.specification;

import com.archiv.ArchivMgnSystem.models.Transaction;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.type.filter.AspectJTypeFilter;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

public class TransactionFilter {

    public static Specification<Transaction> byFilter (String searchValue) {

        return ((root, query, criteriaBuilder) -> {
            if (searchValue == null || searchValue.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            String searchPattern = "%" + searchValue.toLowerCase() + "%";

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchPattern));

            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("note")), searchPattern));

            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("transactionStatus").as(String.class)), searchPattern));

            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("transactionType").as(String.class)), searchPattern));

            if (root.getJoins().stream().noneMatch(j -> j.getAttribute().getName().equals("user"))) {
                root.join("user", JoinType.LEFT);
            }
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.join("user", JoinType.LEFT).get("name")), searchPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.join("user", JoinType.LEFT).get("email")), searchPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.join("user", JoinType.LEFT).get("phoneNumber")), searchPattern));

            if (root.getJoins().stream().noneMatch(j -> j.getAttribute().getName().equals("item"))) {
                root.join("item", JoinType.LEFT);
            }
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.join("item", JoinType.LEFT).get("name")), searchPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.join("item", JoinType.LEFT).get("sku")), searchPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.join("item", JoinType.LEFT).get("description")), searchPattern));

            if (root.getJoins().stream().noneMatch(j -> j.getAttribute().getName().equals("item")) && root.join("item").getJoins().stream().noneMatch(j -> j.getAttribute().getName().equals("categroy"))) {
                root.join("item", JoinType.LEFT).join("category", JoinType.LEFT);
            }
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.join("category", JoinType.LEFT).join("category", JoinType.LEFT).get("name")), searchPattern));

            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        });

    }
    public static Specification<Transaction> byMonthAndYear(int month, int year) {
        return (root, query, criteriaBuilder) -> {
            Expression<Integer> monthExpression= criteriaBuilder.function("month", Integer.class, root.get("createdAt"));
            Expression<Integer> yearExpression= criteriaBuilder.function("year", Integer.class, root.get("createdAt"));

            Predicate monthPredicate = criteriaBuilder.equal(monthExpression, month);
            Predicate yearPredicate = criteriaBuilder.equal(yearExpression, year);

            return criteriaBuilder.and(monthPredicate, yearPredicate);

        };

    }
}
