package com.archiv.ArchivMgnSystem.controllers;

import com.archiv.ArchivMgnSystem.dtos.ActivityLogPageDTO;
import com.archiv.ArchivMgnSystem.dtos.AdminOverviewDTO;
import com.archiv.ArchivMgnSystem.dtos.BackupStatusDTO;
import com.archiv.ArchivMgnSystem.enums.TransactionStatus;
import com.archiv.ArchivMgnSystem.enums.UserRole;
import com.archiv.ArchivMgnSystem.repositories.TransactionRepository;
import com.archiv.ArchivMgnSystem.repositories.UserRepository;
import com.archiv.ArchivMgnSystem.models.ActivityLog;
import com.archiv.ArchivMgnSystem.services.ActivityLogService;
import com.archiv.ArchivMgnSystem.services.BackupService;
import org.springframework.data.domain.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final BackupService backupService;
    private final ActivityLogService activityLogService;

    /** Anzahl offener Anträge und wartender Registrierungen. */
    @GetMapping("/overview")
    public ResponseEntity<AdminOverviewDTO> getOverview() {
        return ResponseEntity.ok(new AdminOverviewDTO(
                transactionRepository.countByTransactionStatus(TransactionStatus.PENDING),
                userRepository.countByApprovedFalseAndRoleNot(UserRole.ADMIN)));
    }

    /** Verlauf des Archivs, neueste Einträge zuerst. */
    @GetMapping("/log")
    public ResponseEntity<ActivityLogPageDTO> getActivityLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        Page<ActivityLog> result = activityLogService.search(page, size, category, search);
        return ResponseEntity.ok(new ActivityLogPageDTO(result.getContent(), result.getTotalElements()));
    }

    @GetMapping("/backup")
    public ResponseEntity<BackupStatusDTO> getBackupStatus() {
        return ResponseEntity.ok(backupService.status());
    }

    @PostMapping("/backup/run")
    public ResponseEntity<BackupStatusDTO> runBackup() {
        return ResponseEntity.ok(backupService.run());
    }
}
