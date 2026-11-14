package com.archiv.ArchivMgnSystem.services.impl;

import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.dtos.TransactionDTO;
import com.archiv.ArchivMgnSystem.dtos.TransactionEditRequest;
import com.archiv.ArchivMgnSystem.dtos.TransactionRequest;
import com.archiv.ArchivMgnSystem.enums.PhotoTarget;
import com.archiv.ArchivMgnSystem.enums.StockPrecision;
import com.archiv.ArchivMgnSystem.enums.TransactionStatus;
import com.archiv.ArchivMgnSystem.enums.TransactionType;
import com.archiv.ArchivMgnSystem.exceptions.NotFoundException;
import com.archiv.ArchivMgnSystem.models.Category;
import com.archiv.ArchivMgnSystem.models.Item;
import com.archiv.ArchivMgnSystem.models.Transaction;
import com.archiv.ArchivMgnSystem.models.User;
import com.archiv.ArchivMgnSystem.repositories.CategoryRepository;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {


    private final ItemRepository itemRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final TransactionRepository transactionRepository;
    private final ItemService itemService;
    private final CategoryRepository categoryRepository;
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

    @Override
    public Response requestNewItem(String name, String place, Integer quantity, StockPrecision precision, Long categoryId,
                                   String description, MultipartFile imageFile, String note) {
        String trimmedName = requireText(name, "Bitte einen Namen angeben", 255);
        // Der Lagerort darf beim Antrag offen bleiben - spätestens der Admin trägt ihn vor der Freigabe ein.
        checkOptionalText(place, "Der Lagerort", 255);
        String trimmedPlace = place == null || place.isBlank() ? null : place.trim();
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Bitte einen Bestand von 0 oder mehr angeben");
        }
        checkOptionalText(description, "Die Beschreibung", 255);
        checkOptionalText(note, "Die Notiz", 255);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        if (itemRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new IllegalArgumentException("Ein Material mit diesem Namen gibt es bereits");
        }
        if (transactionRepository.existsBySubjectNameIgnoreCaseAndTransactionTypeAndTransactionStatus(
                trimmedName, TransactionType.NEW, TransactionStatus.PENDING)) {
            throw new IllegalArgumentException("Für ein Material mit diesem Namen liegt bereits ein offener Antrag vor");
        }

        User user = userService.getCurrentLoggedInUser();
        boolean hasPhoto = imageFile != null && !imageFile.isEmpty();
        String imageUrl = hasPhoto ? itemService.storeImage(imageFile) : null;

        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.NEW)
                .transactionStatus(TransactionStatus.PENDING)
                .user(user)
                .subjectName(trimmedName)
                .totalItems(quantity)
                .proposedPlace(trimmedPlace)
                .proposedStockPrecision(precision != null ? precision : StockPrecision.EXACT)
                .proposedCategoryId(category.getId())
                .proposedCategoryName(category.getName())
                .proposedDescription(description == null || description.isBlank() ? null : description.trim())
                .proposedImageUrl(imageUrl)
                .photoTarget(hasPhoto ? PhotoTarget.ITEM : null)
                .note(note)
                .build();
        try {
            transactionRepository.save(transaction);
        } catch (RuntimeException e) {
            itemService.deleteImagesAfterCommit(Arrays.asList(imageUrl));
            throw e;
        }
        activityLog.record(ActivityLogService.MATERIAL, "Antrag gestellt: " + describe(transaction));

        return Response.builder()
                .status(200)
                .message("Antrag eingereicht, wartet auf Freigabe durch einen Admin")
                .build();
    }

    @Override
    public Response requestItemDeletion(Long itemId, String note) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item Not Found"));
        checkOptionalText(note, "Die Begründung", 255);
        if (transactionRepository.existsByItemIdAndTransactionTypeAndTransactionStatus(
                item.getId(), TransactionType.DELETE, TransactionStatus.PENDING)) {
            throw new IllegalArgumentException("Für dieses Material wurde das Löschen bereits beantragt");
        }

        User user = userService.getCurrentLoggedInUser();
        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.DELETE)
                .transactionStatus(TransactionStatus.PENDING)
                .item(item)
                .user(user)
                .subjectName(item.getName())
                .totalItems(0)
                .note(note)
                .build();
        transactionRepository.save(transaction);
        activityLog.record(ActivityLogService.MATERIAL, "Antrag gestellt: " + describe(transaction));

        return Response.builder()
                .status(200)
                .message("Antrag eingereicht, wartet auf Freigabe durch einen Admin")
                .build();
    }

    @Override
    @Transactional
    public Response editTransaction(Long transactionId, TransactionEditRequest edit) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction Not Found"));
        if (transaction.getTransactionStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("This request was already processed");
        }

        TransactionType type = transaction.getTransactionType();
        String before = describe(transaction);
        List<String> changes = new ArrayList<>();

        if (edit.getQuantity() != null && !edit.getQuantity().equals(transaction.getTotalItems())) {
            if (type == TransactionType.ADD || type == TransactionType.USE) {
                if (edit.getQuantity() <= 0) {
                    throw new IllegalArgumentException("Bitte eine Menge grösser als 0 angeben");
                }
                if (type == TransactionType.USE && edit.getQuantity() > transaction.getItem().getStockQuantity()) {
                    throw new IllegalArgumentException("Requested quantity exceeds current stock");
                }
                changes.add("Menge " + transaction.getTotalItems() + " → " + edit.getQuantity());
                transaction.setTotalItems(edit.getQuantity());
            } else if (type == TransactionType.NEW) {
                if (edit.getQuantity() < 0) {
                    throw new IllegalArgumentException("Bitte einen Bestand von 0 oder mehr angeben");
                }
                changes.add("Bestand " + transaction.getTotalItems() + " → " + edit.getQuantity());
                transaction.setTotalItems(edit.getQuantity());
            }
        }

        if (edit.getPhotoTarget() != null && transaction.getProposedImageUrl() != null
                && type != TransactionType.NEW && edit.getPhotoTarget() != transaction.getPhotoTarget()) {
            changes.add("Foto gilt für " + (edit.getPhotoTarget() == PhotoTarget.LOCATION ? "Lagerort" : "Material"));
            transaction.setPhotoTarget(edit.getPhotoTarget());
        }

        if (type == TransactionType.NEW) {
            if (edit.getName() != null) {
                String name = requireText(edit.getName(), "Bitte einen Namen angeben", 255);
                if (!name.equals(transaction.getSubjectName())) {
                    boolean onlyCaseChanged = name.equalsIgnoreCase(transaction.getSubjectName());
                    if (itemRepository.existsByNameIgnoreCase(name)) {
                        throw new IllegalArgumentException("Ein Material mit diesem Namen gibt es bereits");
                    }
                    if (!onlyCaseChanged && transactionRepository.existsBySubjectNameIgnoreCaseAndTransactionTypeAndTransactionStatus(
                            name, TransactionType.NEW, TransactionStatus.PENDING)) {
                        throw new IllegalArgumentException("Für ein Material mit diesem Namen liegt bereits ein offener Antrag vor");
                    }
                    changes.add("Name " + transaction.getSubjectName() + " → " + name);
                    transaction.setSubjectName(name);
                }
            }
            if (edit.getPlace() != null) {
                checkOptionalText(edit.getPlace(), "Der Lagerort", 255);
                String place = edit.getPlace().isBlank() ? null : edit.getPlace().trim();
                if (!Objects.equals(place, transaction.getProposedPlace())) {
                    changes.add("Lagerort " + (transaction.getProposedPlace() == null ? "–" : transaction.getProposedPlace())
                            + " → " + (place == null ? "–" : place));
                    transaction.setProposedPlace(place);
                }
            }
            if (edit.getCategoryId() != null && !edit.getCategoryId().equals(transaction.getProposedCategoryId())) {
                Category category = categoryRepository.findById(edit.getCategoryId())
                        .orElseThrow(() -> new NotFoundException("Category not found"));
                changes.add("Kategorie " + transaction.getProposedCategoryName() + " → " + category.getName());
                transaction.setProposedCategoryId(category.getId());
                transaction.setProposedCategoryName(category.getName());
            }
            if (edit.getStockPrecision() != null && edit.getStockPrecision() != transaction.getProposedStockPrecision()) {
                changes.add(edit.getStockPrecision() == StockPrecision.EXACT ? "Bestand genau gezählt" : "Bestand ungefähr");
                transaction.setProposedStockPrecision(edit.getStockPrecision());
            }
            if (edit.getDescription() != null) {
                checkOptionalText(edit.getDescription(), "Die Beschreibung", 255);
                String description = edit.getDescription().isBlank() ? null : edit.getDescription().trim();
                if (!Objects.equals(description, transaction.getProposedDescription())) {
                    changes.add("Beschreibung geändert");
                    transaction.setProposedDescription(description);
                }
            }
        }

        if (!changes.isEmpty()) {
            transaction.setEditedByName(userService.getCurrentLoggedInUser().getUsername());
            transactionRepository.save(transaction);
            String requester = transaction.getUser() != null ? transaction.getUser().getUsername() : "ehemaligem Mitglied";
            activityLog.record(logCategory(type), "Antrag bearbeitet: " + before + " (beantragt von " + requester + "): "
                    + String.join(", ", changes));
        }

        return Response.builder()
                .status(200)
                .message(changes.isEmpty() ? "Nothing changed" : "Request updated")
                .build();
    }

    private static String logCategory(TransactionType type) {
        return switch (type) {
            case ADD, USE -> ActivityLogService.STOCK;
            case PHOTO -> ActivityLogService.PHOTO;
            case NEW, DELETE -> ActivityLogService.MATERIAL;
        };
    }

    private static String requireText(String value, String messageIfMissing, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(messageIfMissing);
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException("Die Eingabe ist zu lang (höchstens " + maxLength + " Zeichen)");
        }
        return trimmed;
    }

    private static void checkOptionalText(String value, String label, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(label + " ist zu lang (höchstens " + maxLength + " Zeichen)");
        }
    }

    /** Kurzbeschreibung eines Antrags für den Verlauf, z.B. "+ 3 × Fussball". */
    private String describe(Transaction transaction) {
        String itemName = transaction.getItem() != null ? transaction.getItem().getName()
                : transaction.getSubjectName() != null ? transaction.getSubjectName() : "?";
        String photo = transaction.getPhotoTarget() == PhotoTarget.LOCATION ? "Lagerort-Foto" : "Foto";
        return switch (transaction.getTransactionType()) {
            case PHOTO -> "neues " + photo + " für \"" + itemName + "\"";
            case NEW -> "neues Material \"" + itemName + "\" aufnehmen (Bestand " + transaction.getTotalItems()
                    + (transaction.getProposedPlace() != null ? ", Lagerort " + transaction.getProposedPlace() : "") + ")";
            case DELETE -> "Material \"" + itemName + "\" löschen";
            case ADD, USE -> (transaction.getTransactionType() == TransactionType.ADD ? "+ " : "− ")
                    + transaction.getTotalItems() + " × \"" + itemName + "\""
                    + (transaction.getProposedImageUrl() != null ? " mit neuem " + photo : "");
        };
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
        TransactionType type = existingTransaction.getTransactionType();
        boolean changesStock = type == TransactionType.ADD || type == TransactionType.USE;
        boolean approved = status == TransactionStatus.COMPLETED;
        String proposed = existingTransaction.getProposedImageUrl();
        String requester = existingTransaction.getUser() != null
                ? existingTransaction.getUser().getUsername() : "ehemaligem Mitglied";
        // Beschreibung vor der Entscheidung festhalten (danach fehlen Foto bzw. Material).
        String description = describe(existingTransaction);
        String outcome = "";
        Long itemToDelete = null;

        if (approved && type == TransactionType.NEW) {
            String name = existingTransaction.getSubjectName();
            if (itemRepository.existsByNameIgnoreCase(name)) {
                throw new IllegalArgumentException("Ein Material mit dem Namen \"" + name
                        + "\" gibt es inzwischen. Der Antrag kann nur noch abgelehnt werden.");
            }
            if (existingTransaction.getProposedPlace() == null || existingTransaction.getProposedPlace().isBlank()) {
                throw new IllegalArgumentException("Es fehlt noch der Lagerort. Bitte den Antrag bearbeiten und einen eintragen.");
            }
            // Wurde die Kategorie inzwischen gelöscht und neu angelegt, zählt der Name.
            Category category = categoryRepository.findById(existingTransaction.getProposedCategoryId())
                    .or(() -> categoryRepository.findFirstByNameIgnoreCase(existingTransaction.getProposedCategoryName()))
                    .orElseThrow(() -> new IllegalArgumentException("Die Kategorie \""
                            + existingTransaction.getProposedCategoryName()
                            + "\" gibt es nicht mehr. Bitte zuerst wieder anlegen oder den Antrag ablehnen."));
            Item created = itemService.createFromProposal(name, existingTransaction.getProposedPlace(),
                    existingTransaction.getTotalItems(), existingTransaction.getProposedStockPrecision(), category,
                    existingTransaction.getProposedDescription(), proposed, requester);
            existingTransaction.setItem(created);
            outcome = ", Artikelnummer " + created.getSku();
        } else if (approved && type == TransactionType.DELETE) {
            // Der Antrag selbst soll erhalten bleiben: erst vom Material lösen,
            // dann das Material samt seinen übrigen Anträgen löschen (siehe unten).
            itemToDelete = item.getId();
            existingTransaction.setItem(null);
        } else if (approved) {
            if (changesStock) {
                int delta = type == TransactionType.ADD
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

        String logCategory = changesStock ? ActivityLogService.STOCK
                : type == TransactionType.PHOTO ? ActivityLogService.PHOTO : ActivityLogService.MATERIAL;
        activityLog.record(logCategory, "Antrag " + (approved ? "freigegeben" : "abgelehnt") + ": "
                + description + " (beantragt von " + requester + ")" + outcome);

        if (itemToDelete != null) {
            transactionRepository.flush();
            itemService.deleteItem(itemToDelete);
        }

        return Response.builder()
                .status(200)
                .message("Transaction Status Successfully Updated")
                .build();
    }
}
