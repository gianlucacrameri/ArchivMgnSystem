package com.archiv.ArchivMgnSystem.repositories;

import com.archiv.ArchivMgnSystem.models.Counter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CounterRepository extends JpaRepository<Counter, String> {

    /** Sperrt die Zeile bis zum Ende der Transaktion, damit keine Nummer doppelt vergeben wird. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Counter c WHERE c.name = :name")
    Optional<Counter> findByNameForUpdate(@Param("name") String name);
}
