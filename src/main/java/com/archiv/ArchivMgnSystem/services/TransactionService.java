package com.archiv.ArchivMgnSystem.services;

import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.dtos.TransactionEditRequest;
import com.archiv.ArchivMgnSystem.dtos.TransactionRequest;
import com.archiv.ArchivMgnSystem.enums.PhotoTarget;
import com.archiv.ArchivMgnSystem.enums.StockPrecision;
import com.archiv.ArchivMgnSystem.enums.TransactionStatus;
import com.archiv.ArchivMgnSystem.enums.TransactionType;
import org.springframework.web.multipart.MultipartFile;

public interface TransactionService {
    Response add(TransactionRequest transactionRequest);

    Response use(TransactionRequest transactionRequest);

    Response getAllTransactions(int page, int size, String filter);

    Response getMyTransactions();

    Response editTransaction(Long transactionId, TransactionEditRequest edit);

    Response requestNewItem(String name, String place, Integer quantity, StockPrecision precision, Long categoryId,
                            String description, MultipartFile imageFile, String note);

    Response requestItemDeletion(Long itemId, String note);

    Response requestPhoto(Long itemId, PhotoTarget target, MultipartFile imageFile, String note);

    Response request(Long itemId, TransactionType type, Integer quantity, PhotoTarget target, MultipartFile imageFile, String note);

    Response getAllTransactionById(Long id);

    Response getAllTransactionByMonthAndYear(int month, int year);

    Response updateTransactionStatus(Long transactionId, TransactionStatus status);
}
