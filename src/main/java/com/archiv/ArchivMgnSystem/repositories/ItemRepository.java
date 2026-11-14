package com.archiv.ArchivMgnSystem.repositories;

import com.archiv.ArchivMgnSystem.models.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    /**
     * Höchste bereits vergebene Artikelnummer (numerischer Teil von "ART-00007" -> 7).
     * Bewusst unabhängig von der technischen DB-ID, damit fehlgeschlagene Inserts
     * oder gelöschte Items keine Lücken/Sprünge in der Artikelnummer verursachen.
     */
    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(sku, 5) AS UNSIGNED)), -1) FROM items WHERE sku LIKE 'ART-%'", nativeQuery = true)
    int findHighestArticleNumber();

    boolean existsByNameIgnoreCase(String name);
}
