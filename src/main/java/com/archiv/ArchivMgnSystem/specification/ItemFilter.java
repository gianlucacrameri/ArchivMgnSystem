package com.archiv.ArchivMgnSystem.specification;

import com.archiv.ArchivMgnSystem.models.Item;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ItemFilter {

    public static Specification<Item> byFilter(String searchValue) {
        return (root, query, criteriaBuilder) -> {
            if (searchValue == null || searchValue.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            String searchPattern = "%" + searchValue.toLowerCase() + "%";

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("sku")), searchPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("place")), searchPattern));
            predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("description"), "")), searchPattern));
            predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.join("category", JoinType.LEFT).get("name")), searchPattern));

            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Item> byCategory(Long categoryId) {
        return (root, query, criteriaBuilder) -> categoryId == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("category").get("id"), categoryId);
    }
}
