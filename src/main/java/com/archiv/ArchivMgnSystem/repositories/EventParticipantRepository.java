package com.archiv.ArchivMgnSystem.repositories;

import com.archiv.ArchivMgnSystem.models.EventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

    Optional<EventParticipant> findByEventIdAndUserId(Long eventId, Long userId);

    void deleteByUserId(Long userId);
}
