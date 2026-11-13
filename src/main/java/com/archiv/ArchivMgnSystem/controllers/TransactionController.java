package com.archiv.ArchivMgnSystem.controllers;

import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.dtos.TransactionRequest;
import com.archiv.ArchivMgnSystem.enums.PhotoTarget;
import com.archiv.ArchivMgnSystem.enums.TransactionStatus;
import org.springframework.web.multipart.MultipartFile;
import com.archiv.ArchivMgnSystem.services.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;


    @PostMapping("/add")
    public ResponseEntity<Response> addInventory(@RequestBody @Valid TransactionRequest transactionRequest) {
        return ResponseEntity.ok(transactionService.add(transactionRequest));
    }

    @PostMapping("/use")
    public ResponseEntity<Response> use(@RequestBody @Valid TransactionRequest transactionRequest) {
        return ResponseEntity.ok(transactionService.use(transactionRequest));
    }

    /** Mitglied schlägt ein neues Foto des Materials oder des Lagerorts vor; ein Admin gibt es frei. */
    @PostMapping("/photo")
    public ResponseEntity<Response> requestPhoto(
            @RequestParam("itemId") Long itemId,
            @RequestParam(value = "target", required = false) PhotoTarget target,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "note", required = false) String note) {
        return ResponseEntity.ok(transactionService.requestPhoto(itemId, target, imageFile, note));
    }

    @GetMapping("/all")
    // Enthält Name/E-Mail/Telefon der Antragsteller - nur für Admins.
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<Response> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size,
            @RequestParam(required = false) String filter) {

        return ResponseEntity.ok(transactionService.getAllTransactions(page, size, filter));
    }

    /** Die eigenen Anträge des angemeldeten Mitglieds samt Status. */
    @GetMapping("/mine")
    public ResponseEntity<Response> getMyTransactions() {
        return ResponseEntity.ok(transactionService.getMyTransactions());
    }

    @GetMapping("/{id}")
    // Enthält Name/E-Mail/Telefon der Antragsteller - nur für Admins.
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<Response> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getAllTransactionById(id));
    }

    @GetMapping("/by-month-year")
    // Enthält Name/E-Mail/Telefon der Antragsteller - nur für Admins.
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<Response> getTransactionByMonthAndYear(
            @RequestParam int month,
            @RequestParam int year) {

        return ResponseEntity.ok(transactionService.getAllTransactionByMonthAndYear(month, year));
    }

    @PutMapping("/{transactionId}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<Response> updateTransactionStatus(
            @PathVariable Long transactionId,
            @RequestBody TransactionStatus status) {

        return ResponseEntity.ok(transactionService.updateTransactionStatus(transactionId, status));
    }
}
