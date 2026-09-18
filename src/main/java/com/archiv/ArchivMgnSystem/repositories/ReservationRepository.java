package com.archiv.ArchivMgnSystem.repositories;

import com.archiv.ArchivMgnSystem.models.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
            SELECT COALESCE(SUM(r.quantity), 0) FROM Reservation r
            WHERE r.item.id = :itemId
            AND r.event.id <> :excludedEventId
            AND r.event.startDate <= :endDate
            AND r.event.endDate >= :startDate
            """)
    Integer sumOverlappingReservedQuantity(
            @Param("itemId") Long itemId,
            @Param("excludedEventId") Long excludedEventId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
