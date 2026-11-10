package com.archiv.ArchivMgnSystem.repositories;

import com.archiv.ArchivMgnSystem.models.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    /** Events bleiben bestehen, wenn ihr Ersteller gelöscht wird - sie gehören dann nur noch den Admins. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Event e SET e.createdBy = NULL WHERE e.createdBy.id = :userId")
    void clearCreator(@Param("userId") Long userId);
}
