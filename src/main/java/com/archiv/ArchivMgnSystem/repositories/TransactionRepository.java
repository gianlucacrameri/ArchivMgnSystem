package com.archiv.ArchivMgnSystem.repositories;

import com.archiv.ArchivMgnSystem.enums.TransactionStatus;
import com.archiv.ArchivMgnSystem.models.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    void deleteByItemId(long itemId);

    void deleteByItemCategoryId(Long categoryId);

    long countByTransactionStatus(TransactionStatus transactionStatus);

    @EntityGraph(attributePaths = "item")
    List<Transaction> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    /** Die Bestandshistorie bleibt erhalten, wenn ein Mitglied gelöscht wird - nur ohne Namen. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Transaction t SET t.user = NULL WHERE t.user.id = :userId")
    void clearUser(@Param("userId") Long userId);
}
