package com.archiv.ArchivMgnSystem.services;

import com.archiv.ArchivMgnSystem.models.ActivityLog;
import com.archiv.ArchivMgnSystem.repositories.ActivityLogRepository;
import com.archiv.ArchivMgnSystem.security.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** Schreibt und liest den Verlauf des Archivs (wer hat wann was getan). */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {

    public static final String MATERIAL = "MATERIAL";
    public static final String STOCK = "BESTAND";
    public static final String PHOTO = "FOTO";
    public static final String CATEGORY = "KATEGORIE";
    public static final String MEMBER = "MITGLIED";
    public static final String EVENT = "EVENT";

    private static final int MAX_MESSAGE_LENGTH = 600;

    private final ActivityLogRepository activityLogRepository;

    /** Hält eine Aktion des angemeldeten Benutzers fest. */
    public void record(String category, String message) {
        record(category, currentActorName(), message);
    }

    public void record(String category, String actorName, String message) {
        try {
            activityLogRepository.save(ActivityLog.builder()
                    .createdAt(LocalDateTime.now())
                    .category(category)
                    .actorName(actorName == null || actorName.isBlank() ? "Unbekannt" : actorName)
                    .message(message.length() > MAX_MESSAGE_LENGTH ? message.substring(0, MAX_MESSAGE_LENGTH) : message)
                    .build());
        } catch (RuntimeException e) {
            // Der Verlauf ist ein Zusatz: scheitert der Eintrag, soll die
            // eigentliche Aktion deswegen nicht fehlschlagen.
            log.warn("Could not write activity log entry: {}", e.getMessage());
        }
    }

    public Page<ActivityLog> search(int page, int size, String category, String search) {
        String normalizedCategory = category == null || category.isBlank() ? null : category.trim().toUpperCase();
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return activityLogRepository.search(normalizedCategory, normalizedSearch,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), Sort.by(Sort.Direction.DESC, "id")));
    }

    private String currentActorName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser authUser) {
            return authUser.getUser().getUsername();
        }
        return null;
    }
}
