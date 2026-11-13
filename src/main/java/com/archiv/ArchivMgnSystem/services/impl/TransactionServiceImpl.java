package com.archiv.ArchivMgnSystem.services.impl;

import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.dtos.TransactionDTO;
import com.archiv.ArchivMgnSystem.dtos.TransactionRequest;
import com.archiv.ArchivMgnSystem.enums.PhotoTarget;
import com.archiv.ArchivMgnSystem.enums.TransactionStatus;
import com.archiv.ArchivMgnSystem.enums.TransactionType;
import com.archiv.ArchivMgnSystem.exceptions.NotFoundException;
import com.archiv.ArchivMgnSystem.models.Item;
import com.archiv.ArchivMgnSystem.models.Transaction;
import com.archiv.ArchivMgnSystem.models.User;
import com.archiv.ArchivMgnSystem.repositories.ItemRepository;
import com.archiv.ArchivMgnSystem.repositories.TransactionRepository;
import com.archiv.ArchivMgnSystem.services.ActivityLogService;
import com.archiv.ArchivMgnSystem.services.ItemService;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {


    private final ItemRepository itemRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final TransactionRepository transactionRepository;
    private final ItemService itemService;
    private final ActivityLogService activityLog;

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
        activityLog.record(ActivityLogService.STOCK, "Antrag gestellt: " + describe(transaction));
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
        activityLog.record(ActivityLogService.STOCK, "Antrag gestellt: " + describe(transaction));
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
    public Response requestPhoto(Long itemId, PhotoTarget target, MultipartFile imageFile, String note) {
        return request(itemId, TransactionType.PHOTO, null, target, imageFile, note);
    }

    /**
     * Ein Antrag kann den Bestand ändern (ADD/USE mit Menge), ein neues Foto
     * vorschlagen oder beides zugleich. Ohne Bestandsänderung ist es ein
     * reiner Foto-Antrag (PHOTO).
     */
    @Override
    public Response request(Long itemId, TransactionType type, Integer quantity, PhotoTarget target,
                            MultipartFile imageFile, String note) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item Not Found"));

        boolean hasPhoto = imageFile != null && !imageFile.isEmpty();
        boolean changesStock = type == TransactionType.ADD || type == TransactionType.USE;

        if (changesStock) {
            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("Bitte eine Menge grösser als 0 angeben");
            }
            if (type == TransactionType.USE && quantity > item.getStockQuantity()) {
                throw new IllegalArgumentException("Requested quantity exceeds current stock");
            }
        } else if (!hasPhoto) {
            throw new IllegalArgumentException("Bitte ein Foto auswählen");
        }
        if (note != null && note.length() > 255) {
            throw new IllegalArgumentException("Die Notiz ist zu lang (höchstens 255 Zeichen)");
        }

        User user = userService.getCurrentLoggedInUser();
        String imageUrl = hasPhoto ? itemService.storeImage(imageFile) : null;

        Transaction transaction = Transaction.builder()
                .transactionType(changesStock ? type : TransactionType.PHOTO)
                .transactionStatus(TransactionStatus.PENDING)
                .item(item)
                .user(user)
                .totalItems(changesStock ? quantity : 0)
                .photoTarget(hasPhoto ? (target != null ? target : PhotoTarget.ITEM) : null)
                .proposedImageUrl(imageUrl)
                .note(note)
                .build();
        try {
            transactionRepository.save(transaction);
        } catch (RuntimeException e) {
            // Antrag nicht gespeichert - das Foto würde sonst ohne Antrag liegen bleiben.
            itemService.deleteImagesAfterCommit(Arrays.asList(imageUrl));
            throw e;
        }
        activityLog.record(changesStock ? ActivityLogService.STOCK : ActivityLogService.PHOTO,
                "Antrag gestellt: " + describe(transaction));

        return Response.builder()
                .status(200)
                .message("Antrag eingereicht, wartet auf Freigabe durch einen Admin")
                .build();
    }

    /** Kurzbeschreibung eines Antrags für den Verlauf, z.B. "+ 3 × Fussball". */
    private String describe(Transaction transaction) {
        String itemName = transaction.getItem() != null ? transaction.getItem().getName() : "?";
        String photo = transaction.getPhotoTarget() == PhotoTarget.LOCATION ? "Lagerort-Foto" : "Foto";
        if (transaction.getTransactionType() == TransactionType.PHOTO) {
            return "neues " + photo + " für \"" + itemName + "\"";
        }
        return (transaction.getTransactionType() == TransactionType.ADD ? "+ " : "− ")
                + transaction.getTotalItems() + " × \"" + itemName + "\""
                + (transaction.getProposedImageUrl() != null ? " mit neuem " + photo : "");
    }

    @Override
    @Transactional
    public Response updateTransactionStatus(Long transactionId, TransactionStatus status) {
        Transaction existingTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction Not Found"));

        if (existingTransaction.getTransactionStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("This request was already processed");
        }
        if (status != TransactionStatus.COMPLETED && status != TransactionStatus.CANCELED) {
            throw new IllegalArgumentException("Status must be either COMPLETED or CANCELED");
        }

        Item item = existingTransaction.getItem();
        boolean isPhoto = existingTransaction.getTransactionType() == TransactionType.PHOTO;
        String proposed = existingTransaction.getProposedImageUrl();
        // Beschreibung vor der Entscheidung festhalten (bei Ablehnung entfällt das Foto).
        String description = describe(existingTransaction);
        String outcome = "";

        if (status == TransactionStatus.COMPLETED) {
            if (!isPhoto) {
                int delta = existingTransaction.getTransactionType() == TransactionType.ADD
                        ? existingTransaction.getTotalItems()
                        : -existingTransaction.getTotalItems();

                int oldQuantity = item.getStockQuantity();
                int newQuantity = oldQuantity + delta;
                if (newQuantity < 0) {
                    throw new IllegalArgumentException("Not enough stock available to approve this request");
                }
                item.setStockQuantity(newQuantity);
                outcome = ", Bestand " + oldQuantity + " → " + newQuantity;
            }
            if (proposed != null) {
                // Das vorgeschlagene Foto wird zum Foto des Materials, das bisherige entfällt.
                if (existingTransaction.getPhotoTarget() == PhotoTarget.LOCATION) {
                    itemService.deleteImagesAfterCommit(Arrays.asList(item.getLocationImageUrl()));
                    item.setLocationImageUrl(proposed);
                } else {
                    itemService.deleteImagesAfterCommit(Arrays.asList(item.getImageUrl()));
                    item.setImageUrl(proposed);
                }
            }
            itemRepository.save(item);
        } else if (proposed != null) {
            // Abgelehnt: das vorgeschlagene Foto wird nicht mehr gebraucht.
            itemService.deleteImagesAfterCommit(Arrays.asList(proposed));
            existingTransaction.setProposedImageUrl(null);
        }

        User decider = userService.getCurrentLoggedInUser();
        existingTransaction.setTransactionStatus(status);
        existingTransaction.setDecidedByName(decider.getUsername());
        existingTransaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(existingTransaction);

        String requester = existingTransaction.getUser() != null
                ? existingTransaction.getUser().getUsername() : "ehemaligem Mitglied";
        activityLog.record(isPhoto ? ActivityLogService.PHOTO : ActivityLogService.STOCK,
                "Antrag " + (status == TransactionStatus.COMPLETED ? "freigegeben" : "abgelehnt") + ": "
                        + description + " (beantragt von " + requester + ")" + outcome);

        return Response.builder()
                .status(200)
                .message("Transaction Status Successfully Updated")
                .build();
    }
}
