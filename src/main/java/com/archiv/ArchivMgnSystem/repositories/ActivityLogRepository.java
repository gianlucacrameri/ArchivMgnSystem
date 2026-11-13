package com.archiv.ArchivMgnSystem.repositories;

import com.archiv.ArchivMgnSystem.models.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    @Query("""
            SELECT a FROM ActivityLog a
            WHERE (:category IS NULL OR a.category = :category)
            AND (:search IS NULL
                 OR LOWER(a.message) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(a.actorName) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<ActivityLog> search(@Param("category") String category, @Param("search") String search, Pageable pageable);
}
