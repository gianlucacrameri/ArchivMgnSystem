package com.archiv.ArchivMgnSystem.services.impl;

import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.dtos.TransactionDTO;
import com.archiv.ArchivMgnSystem.dtos.TransactionRequest;
import com.archiv.ArchivMgnSystem.enums.TransactionStatus;
import com.archiv.ArchivMgnSystem.enums.TransactionType;
import com.archiv.ArchivMgnSystem.exceptions.NotFoundException;
import com.archiv.ArchivMgnSystem.models.Item;
import com.archiv.ArchivMgnSystem.models.Transaction;
import com.archiv.ArchivMgnSystem.models.User;
import com.archiv.ArchivMgnSystem.repositories.ItemRepository;
import com.archiv.ArchivMgnSystem.repositories.TransactionRepository;
import com.archiv.ArchivMgnSystem.services.TransactionService;
import com.archiv.ArchivMgnSystem.services.UserService;
import com.archiv.ArchivMgnSystem.specification.TransactionFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {


    private final ItemRepository itemRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final TransactionRepository transactionRepository;

    private static final int MAX_OWN_TRANSACTIONS = 100;

    @Override
    public Response add(TransactionRequest transactionRequest) {
        Long itemId = transactionRequest.getItemId();
        Integer quantity = transactionRequest.getQuantity();

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item Not Found"));

        User user = userService.getCurrentLoggedInUser();

        //create a pending request; stock is only updated once an admin approves it
        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.ADD)
                .transactionStatus(TransactionStatus.PENDING)
                .item(item)
                .user(user)
                .totalItems(quantity)
                .description(transactionRequest.getDescription())
                .note(transactionRequest.getNote())
                .build();

        transactionRepository.save(transaction);
        return Response.builder()
                .status(200)
                .message("Bestandsänderung beantragt, wartet auf Freigabe durch einen Admin")
                .build();

    }

    @Override
    public Response use(TransactionRequest transactionRequest) {
        Long itemId = transactionRequest.getItemId();
        Integer quantity = transactionRequest.getQuantity();

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item Not Found"));

        if (quantity > item.getStockQuantity()) {
            throw new IllegalArgumentException("Requested quantity exceeds current stock");
        }

        User user = userService.getCurrentLoggedInUser();

        //create a pending request; stock is only updated once an admin approves it
        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.USE)
                .transactionStatus(TransactionStatus.PENDING)
                .item(item)
                .user(user)
                .totalItems(quantity)
                .description(transactionRequest.getDescription())
                .note(transactionRequest.getNote())
                .build();

        transactionRepository.save(transaction);
        return Response.builder()
                .status(200)
                .message("Bestandsänderung beantragt, wartet auf Freigabe durch einen Admin")
                .build();
    }


    @Override
    public Response getAllTransactions(int page, int size, String filter) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        //user the Transaction specification
        Specification<Transaction> spec = TransactionFilter.byFilter(filter);
        Page<Transaction> transactionPage = transactionRepository.findAll(spec, pageable);

        List<TransactionDTO> transactionDTOS = modelMapper.map(transactionPage.getContent(), new TypeToken<List<TransactionDTO>>() {
        }.getType());

        transactionDTOS.forEach(transactionDTO -> {
            if (transactionDTO.getUser() != null) {
                transactionDTO.getUser().setTransactions(null);
            }
        });

        return Response.builder()
                .status(200)
                .message("success")
                .transactions(transactionDTOS)
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .build();

    }


    @Override
    public Response getMyTransactions() {
        User user = userService.getCurrentLoggedInUser();
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByIdDesc(
                user.getId(), PageRequest.of(0, MAX_OWN_TRANSACTIONS));

        List<TransactionDTO> transactionDTOS = modelMapper.map(transactions, new TypeToken<List<TransactionDTO>>() {
        }.getType());
        // Der Antragsteller ist der Aufrufer selbst - seine Daten müssen nicht mit.
        transactionDTOS.forEach(transactionDTO -> transactionDTO.setUser(null));

        return Response.builder()
                .status(200)
                .message("success")
                .transactions(transactionDTOS)
                .build();
    }

    @Override
    public Response getAllTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction Not Found"));

        TransactionDTO transactionDTO = modelMapper.map(transaction, TransactionDTO.class);

        if (transactionDTO.getUser() != null) {
            transactionDTO.getUser().setTransactions(null);
        }

        return Response.builder()
                .status(200)
                .message("success")
                .transaction(transactionDTO)
                .build();

    }

    @Override
    public Response getAllTransactionByMonthAndYear(int month, int year) {
        List<Transaction> transactions = transactionRepository.findAll(TransactionFilter.byMonthAndYear(month, year));

        List<TransactionDTO> transactionDTOS = modelMapper.map(transactions, new TypeToken<List<TransactionDTO>>() {
        }.getType());

        transactionDTOS.forEach(transactionDTO -> {
            if (transactionDTO.getUser() != null) {
                transactionDTO.getUser().setTransactions(null);
            }
        });

        return Response.builder()
                .status(200)
                .message("success")
                .transactions(transactionDTOS)
                .build();
    }

    @Override
    public Response updateTransactionStatus(Long transactionId, TransactionStatus status) {
        Transaction existingTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction Not Found"));

        if (existingTransaction.getTransactionStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("This request was already processed");
        }

        if (status == TransactionStatus.COMPLETED) {
            Item item = existingTransaction.getItem();
            int delta = existingTransaction.getTransactionType() == TransactionType.ADD
                    ? existingTransaction.getTotalItems()
                    : -existingTransaction.getTotalItems();

            int newQuantity = item.getStockQuantity() + delta;
            if (newQuantity < 0) {
                throw new IllegalArgumentException("Not enough stock available to approve this request");
            }
            item.setStockQuantity(newQuantity);
            itemRepository.save(item);
        } else if (status != TransactionStatus.CANCELED) {
            throw new IllegalArgumentException("Status must be either COMPLETED or CANCELED");
        }

        existingTransaction.setTransactionStatus(status);
        existingTransaction.setUpdatedAt(LocalDateTime.now());

        transactionRepository.save(existingTransaction);

        return Response.builder()
                .status(200)
                .message("Transaction Status Successfully Updated")
                .build();
    }
}
