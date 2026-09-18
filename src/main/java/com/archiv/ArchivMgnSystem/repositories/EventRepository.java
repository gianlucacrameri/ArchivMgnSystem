package com.archiv.ArchivMgnSystem.repositories;

import com.archiv.ArchivMgnSystem.models.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
