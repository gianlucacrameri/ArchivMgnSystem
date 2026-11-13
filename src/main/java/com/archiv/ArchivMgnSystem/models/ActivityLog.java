package com.archiv.ArchivMgnSystem.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Ein Eintrag im Verlauf des Archivs: wer hat wann was getan. Name und
 * Beschreibung sind bewusst als Text festgehalten - der Eintrag bleibt
 * lesbar, auch wenn das Material oder das Mitglied später gelöscht wird.
 */
@Entity
@Table(name = "activity_log", indexes = @Index(name = "idx_activity_log_created_at", columnList = "createdAt"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** Bereich: MATERIAL, BESTAND, FOTO, KATEGORIE, MITGLIED oder EVENT. */
    @Column(nullable = false, length = 20)
    private String category;

    @Column(nullable = false)
    private String actorName;

    @Column(nullable = false, length = 600)
    private String message;
}
