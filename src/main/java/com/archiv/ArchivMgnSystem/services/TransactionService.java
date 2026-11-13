package com.archiv.ArchivMgnSystem.services;

import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.dtos.TransactionRequest;
import com.archiv.ArchivMgnSystem.enums.PhotoTarget;
import com.archiv.ArchivMgnSystem.enums.TransactionStatus;
import org.springframework.web.multipart.MultipartFile;

public interface TransactionService {
    Response add(TransactionRequest transactionRequest);

    Response use(TransactionRequest transactionRequest);

    Response getAllTransactions(int page, int size, String filter);

    Response getMyTransactions();

    Response requestPhoto(Long itemId, PhotoTarget target, MultipartFile imageFile, String note);

    Response getAllTransactionById(Long id);

    Response getAllTransactionByMonthAndYear(int month, int year);

    Response updateTransactionStatus(Long transactionId, TransactionStatus status);
}
