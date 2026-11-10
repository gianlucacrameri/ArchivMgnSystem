package com.archiv.ArchivMgnSystem.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BackupStatusDTO {
    /** false = kein Backup-Ordner konfiguriert, es wird nichts gesichert. */
    private boolean enabled;
    private String backupDirectory;
    private String photoDirectory;
    private int keepDays;

    /** Neueste vorhandene Datenbank-Sicherung. */
    private LocalDateTime lastBackupAt;
    private Long lastBackupSizeBytes;
    private int databaseBackupCount;
    private long photoCount;

    /** Fehlermeldung des letzten Versuchs seit dem Start des Servers, falls er scheiterte. */
    private String lastError;
    private LocalDateTime lastErrorAt;
}
