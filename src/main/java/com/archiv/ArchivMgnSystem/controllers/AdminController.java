package com.archiv.ArchivMgnSystem.controllers;

import com.archiv.ArchivMgnSystem.dtos.AdminOverviewDTO;
import com.archiv.ArchivMgnSystem.dtos.BackupStatusDTO;
import com.archiv.ArchivMgnSystem.enums.TransactionStatus;
import com.archiv.ArchivMgnSystem.enums.UserRole;
import com.archiv.ArchivMgnSystem.repositories.TransactionRepository;
import com.archiv.ArchivMgnSystem.repositories.UserRepository;
import com.archiv.ArchivMgnSystem.services.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final BackupService backupService;

    /** Anzahl offener Anträge und wartender Registrierungen. */
    @GetMapping("/overview")
    public ResponseEntity<AdminOverviewDTO> getOverview() {
        return ResponseEntity.ok(new AdminOverviewDTO(
                transactionRepository.countByTransactionStatus(TransactionStatus.PENDING),
                userRepository.countByApprovedFalseAndRoleNot(UserRole.ADMIN)));
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
