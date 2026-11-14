package com.archiv.ArchivMgnSystem.services.impl;


import com.archiv.ArchivMgnSystem.dtos.ItemDTO;
import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.enums.ImageSize;
import com.archiv.ArchivMgnSystem.enums.StockPrecision;
import com.archiv.ArchivMgnSystem.exceptions.NotFoundException;
import com.archiv.ArchivMgnSystem.models.Category;
import com.archiv.ArchivMgnSystem.models.Counter;
import com.archiv.ArchivMgnSystem.models.Item;
import com.archiv.ArchivMgnSystem.repositories.CategoryRepository;
import com.archiv.ArchivMgnSystem.repositories.CounterRepository;
import com.archiv.ArchivMgnSystem.repositories.ItemRepository;
import com.archiv.ArchivMgnSystem.repositories.ReservationRepository;
import com.archiv.ArchivMgnSystem.repositories.TransactionRepository;
import com.archiv.ArchivMgnSystem.services.ActivityLogService;
import com.archiv.ArchivMgnSystem.services.ItemService;
import com.archiv.ArchivMgnSystem.specification.ItemFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final ModelMapper modelMapper;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final ReservationRepository reservationRepository;
    private final CounterRepository counterRepository;
    private final ActivityLogService activityLog;

    @Value("${app.upload-dir:${user.dir}/uploads/items}")
    private String uploadDir;

    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

    private static final String VARIANT_DIRECTORY = "variants";
    private static final Object VARIANT_LOCK = new Object();
    // Schutz vor "Dekompressionsbomben": kleine Datei, riesige Pixelfläche.
    private static final long MAX_VARIANT_SOURCE_PIXELS = 50_000_000L;

    // SVG ist bewusst nicht erlaubt: SVG-Dateien können <script> enthalten und
    // würden beim Öffnen in einem eigenen Tab (blob: URL, gleiche Origin wie die
    // App) im Browser ausgeführt - Stored-XSS-Risiko trotz "image/*"-Content-Type.
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    @Override
    @Transactional
    public Response saveItem(ItemDTO itemDTO, MultipartFile imageFile, MultipartFile locationImageFile) {

        Category category = categoryRepository.findById(itemDTO.getCategoryId())
                .orElseThrow(() ->new NotFoundException("Category not found"));

        //map dto to product entity
        Item itemToSave = Item.builder()
                .name(itemDTO.getName())
                .stockQuantity(itemDTO.getStockQuantity())
                .stockPrecision(itemDTO.getStockPrecision() != null ? itemDTO.getStockPrecision() : StockPrecision.EXACT)
                .place(itemDTO.getPlace())
                .description(itemDTO.getDescription())
                .category(category)
                .createdAt(LocalDateTime.now())
                .build();

        List<String> savedImages = new ArrayList<>();
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                itemToSave.setImageUrl(saveImage(imageFile));
                savedImages.add(itemToSave.getImageUrl());
            }
            if (locationImageFile != null && !locationImageFile.isEmpty()) {
                itemToSave.setLocationImageUrl(saveImage(locationImageFile));
                savedImages.add(itemToSave.getLocationImageUrl());
            }

            itemToSave.setSku(String.format("ART-%05d", nextArticleNumber()));
            itemRepository.saveAndFlush(itemToSave);
        } catch (RuntimeException e) {
            // Item wurde nicht angelegt (z.B. Name schon vergeben) - die bereits
            // gespeicherten Fotos würden sonst ohne Item liegen bleiben.
            savedImages.forEach(this::deleteImageFiles);
            throw e;
        }

        activityLog.record(ActivityLogService.MATERIAL, "Material \"" + itemToSave.getName() + "\" (" + itemToSave.getSku()
                + ") angelegt, Bestand " + itemToSave.getStockQuantity() + ", Lagerort " + itemToSave.getPlace());

        return Response.builder()
                .message("Item successfully saved")
                .build();
    }

    @Override
    @Transactional
    public Item createFromProposal(String name, String place, int quantity, StockPrecision precision,
                                   Category category, String description, String imageUrl, String requestedBy) {
        Item item = Item.builder()
                .name(name)
                .place(place)
                .stockQuantity(quantity)
                .stockPrecision(precision != null ? precision : StockPrecision.EXACT)
                .category(category)
                .description(description)
                .imageUrl(imageUrl)
                .createdAt(LocalDateTime.now())
                .build();
        item.setSku(String.format("ART-%05d", nextArticleNumber()));
        itemRepository.saveAndFlush(item);

        activityLog.record(ActivityLogService.MATERIAL, "Material \"" + item.getName() + "\" (" + item.getSku()
                + ") angelegt (beantragt von " + requestedBy + "), Bestand " + item.getStockQuantity()
                + ", Lagerort " + item.getPlace());
        return item;
    }

    /**
     * Artikelnummern werden fortlaufend ab 0 vergeben - unabhängig von der
     * technischen DB-ID. Der Zähler läuft auch nach dem Löschen eines Items
     * weiter, damit eine einmal vergebene Nummer (z.B. auf einem Etikett) nie
     * für ein anderes Material wiederverwendet wird. Scheitert das Anlegen,
     * wird die Nummer mit der Transaktion zurückgerollt und geht nicht verloren.
     */
    private int nextArticleNumber() {
        Counter counter = counterRepository.findByNameForUpdate(Counter.ARTICLE_NUMBER)
                .orElseGet(() -> new Counter(Counter.ARTICLE_NUMBER, 0));
        // Beim ersten Mal (und falls Nummern von Hand vergeben wurden) hinter
        // der höchsten vorhandenen Nummer weitermachen.
        int next = Math.max(counter.getNextValue(), itemRepository.findHighestArticleNumber() + 1);
        counter.setNextValue(next + 1);
        counterRepository.save(counter);
        return next;
    }

    @Override
    @Transactional
    public Response updateItem(ItemDTO itemDTO, MultipartFile imageFile, MultipartFile locationImageFile) {
        Item existingItem = itemRepository.findById(itemDTO.getItemId())
                .orElseThrow(() -> new NotFoundException("Item not found"));

        // Ersetzte Fotos werden nach erfolgreichem Speichern vom Server entfernt.
        String nameBefore = existingItem.getName();
        List<String> replacedImages = new ArrayList<>();
        List<String> newImages = new ArrayList<>();
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                replacedImages.add(existingItem.getImageUrl());
                existingItem.setImageUrl(saveImage(imageFile));
                newImages.add(existingItem.getImageUrl());
            }
            if (locationImageFile != null && !locationImageFile.isEmpty()) {
                replacedImages.add(existingItem.getLocationImageUrl());
                existingItem.setLocationImageUrl(saveImage(locationImageFile));
                newImages.add(existingItem.getLocationImageUrl());
            }
            List<String> changes = applyChanges(existingItem, itemDTO);
            if (imageFile != null && !imageFile.isEmpty()) {
                changes.add("Foto ersetzt");
            }
            if (locationImageFile != null && !locationImageFile.isEmpty()) {
                changes.add("Lagerort-Foto ersetzt");
            }
            itemRepository.saveAndFlush(existingItem);
            if (!changes.isEmpty()) {
                activityLog.record(ActivityLogService.MATERIAL, "Material \"" + nameBefore + "\" (" + existingItem.getSku()
                        + ") bearbeitet: " + String.join(", ", changes));
            }
        } catch (RuntimeException e) {
            newImages.forEach(this::deleteImageFiles);
            throw e;
        }
        deleteImagesAfterCommit(replacedImages);

        return Response.builder()
                .status(200)
                .message("Item updated successfully")
                .build();
    }

    /** Übernimmt die Änderungen und gibt zurück, was sich geändert hat (für den Verlauf). */
    private List<String> applyChanges(Item existingItem, ItemDTO itemDTO) {
        List<String> changes = new ArrayList<>();

        if (itemDTO.getCategoryId() > 0
                && (existingItem.getCategory() == null || !existingItem.getCategory().getId().equals(itemDTO.getCategoryId()))) {
            Category category = categoryRepository.findById(itemDTO.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            changes.add("Kategorie " + (existingItem.getCategory() == null ? "–" : existingItem.getCategory().getName())
                    + " → " + category.getName());
            existingItem.setCategory(category);
        }

        if (itemDTO.getName() != null && !itemDTO.getName().isBlank() && !itemDTO.getName().equals(existingItem.getName())) {
            changes.add("Name → " + itemDTO.getName());
            existingItem.setName(itemDTO.getName());
        }

        if (itemDTO.getDescription() != null && !itemDTO.getDescription().isBlank()
                && !itemDTO.getDescription().equals(existingItem.getDescription())) {
            changes.add("Beschreibung geändert");
            existingItem.setDescription(itemDTO.getDescription());
        }

        if (itemDTO.getPlace() != null && !itemDTO.getPlace().isBlank() && !itemDTO.getPlace().equals(existingItem.getPlace())) {
            changes.add("Lagerort " + existingItem.getPlace() + " → " + itemDTO.getPlace());
            existingItem.setPlace(itemDTO.getPlace());
        }

        if (itemDTO.getStockQuantity() != null && itemDTO.getStockQuantity() >= 0
                && !itemDTO.getStockQuantity().equals(existingItem.getStockQuantity())) {
            changes.add("Bestand " + existingItem.getStockQuantity() + " → " + itemDTO.getStockQuantity());
            existingItem.setStockQuantity(itemDTO.getStockQuantity());
        }

        if (itemDTO.getStockPrecision() != null && itemDTO.getStockPrecision() != existingItem.getStockPrecision()) {
            changes.add(itemDTO.getStockPrecision() == StockPrecision.EXACT ? "Bestand genau gezählt" : "Bestand ungefähr");
            existingItem.setStockPrecision(itemDTO.getStockPrecision());
        }

        return changes;
    }

    @Override
    public Response getAllItems(int page, int size, String filter, Long categoryId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        Specification<Item> spec = ItemFilter.byFilter(filter).and(ItemFilter.byCategory(categoryId));

        Page<Item> itemPage = itemRepository.findAll(spec, pageable);

        List<ItemDTO> itemDTOList = modelMapper.map(itemPage.getContent(), new TypeToken<List<ItemDTO>>()
        {}.getType());

        return Response.builder()
                .status(200)
                .message("success")
                .items(itemDTOList)
                .totalElements(itemPage.getTotalElements())
                .totalPages(itemPage.getTotalPages())
                .build();
    }

    @Override
    public Response getItemsById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found"));

        return Response.builder()
                .status(200)
                .message("success")
                .item(modelMapper.map(item, ItemDTO.class))
                .build();
    }

    @Override
    @Transactional
    public Response deleteItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found"));
        deleteImagesAfterCommit(Arrays.asList(item.getImageUrl(), item.getLocationImageUrl()));
        // auch die Fotos aus Foto-Anträgen zu diesem Material
        deleteImagesAfterCommit(transactionRepository.findProposedImageUrlsByItemId(id));
        activityLog.record(ActivityLogService.MATERIAL, "Material \"" + item.getName() + "\" (" + item.getSku()
                + ") gelöscht, letzter Bestand " + item.getStockQuantity());

        // Item hat Fremdschlüssel in transactions (Bestandshistorie) und
        // reservations (Event-Reservierungen) - ohne Item hat weder das eine
        // noch das andere einen Sinn, also beides mitlöschen statt an der
        // DB-Constraint zu scheitern.
        reservationRepository.deleteByItemId(id);
        transactionRepository.deleteByItemId(id);
        itemRepository.deleteById(id);

        return Response.builder()
                .status(200)
                .message("Item deleted successfully")
                .build();

    }

    @Override
    public Resource loadImage(String filename, ImageSize size) {
        Path baseDir = Paths.get(uploadDir).normalize().toAbsolutePath();
        Path original = baseDir.resolve(filename).normalize();

        // Nur Dateien direkt im Upload-Ordner: kein "..", kein Unterordner.
        if (!baseDir.equals(original.getParent())) {
            throw new IllegalArgumentException("Invalid file path");
        }
        if (!Files.isRegularFile(original) || !Files.isReadable(original)) {
            throw new NotFoundException("Image not found");
        }

        return new FileSystemResource(size == null ? original : variantOf(original, size));
    }

    /**
     * Liefert die verkleinerte JPEG-Variante eines Fotos und erzeugt sie beim
     * ersten Zugriff. Klappt das nicht (unbekanntes Format, zu gross, defekt),
     * wird einfach das Original ausgeliefert.
     */
    private Path variantOf(Path original, ImageSize size) {
        String name = original.getFileName().toString();
        String lowerName = name.toLowerCase(Locale.ROOT);
        boolean jpeg = lowerName.endsWith(".jpg");
        boolean needsFlattening = lowerName.endsWith(".png") || lowerName.endsWith(".gif");
        if (!jpeg && !needsFlattening) {
            return original; // z.B. WebP: die JVM bringt dafür keinen Decoder mit
        }

        Path variantDir = original.getParent().resolve(VARIANT_DIRECTORY);
        Path variant = variantDir.resolve(size.name().toLowerCase(Locale.ROOT) + "-" + name + ".jpg");
        if (Files.isRegularFile(variant)) {
            return variant;
        }

        // Ein Bild nach dem anderen: 40 Handyfotos gleichzeitig zu dekodieren
        // (je ~50-100 MB im Speicher) würde den Heap sprengen.
        synchronized (VARIANT_LOCK) {
            if (Files.isRegularFile(variant)) {
                return variant;
            }
            Path tmp = null;
            try {
                int[] dimensions = readDimensions(original);
                if ((long) dimensions[0] * dimensions[1] > MAX_VARIANT_SOURCE_PIXELS) {
                    return original;
                }

                Files.createDirectories(variantDir);
                tmp = Files.createTempFile(variantDir, "tmp-", ".jpg");

                // Thumbnails.of(File) wertet die EXIF-Drehung von Handyfotos aus.
                Thumbnails.Builder<?> builder = jpeg
                        ? Thumbnails.of(original.toFile())
                        : Thumbnails.of(flattenOnWhite(original));
                if (Math.max(dimensions[0], dimensions[1]) <= size.getMaxEdge()) {
                    builder.scale(1.0); // kleine Bilder nicht hochskalieren
                } else {
                    builder.size(size.getMaxEdge(), size.getMaxEdge());
                }
                builder.outputFormat("jpg").outputQuality(size.getQuality()).toFile(tmp.toFile());

                Files.move(tmp, variant, StandardCopyOption.REPLACE_EXISTING);
                return variant;
            } catch (IOException | RuntimeException | OutOfMemoryError e) {
                log.warn("Could not create {} variant of {}: {}", size, name, e.toString());
                deleteQuietly(tmp);
                return original;
            }
        }
    }

    private int[] readDimensions(Path image) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(image.toFile())) {
            Iterator<ImageReader> readers = input == null ? null : ImageIO.getImageReaders(input);
            if (readers == null || !readers.hasNext()) {
                throw new IOException("Unsupported image format");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                return new int[]{reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        }
    }

    /** JPEG kennt keine Transparenz - transparente PNG/GIF-Flächen sonst schwarz. */
    private BufferedImage flattenOnWhite(Path image) throws IOException {
        BufferedImage source = ImageIO.read(image.toFile());
        if (source == null) {
            throw new IOException("Unsupported image format");
        }
        BufferedImage flattened = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = flattened.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return flattened;
    }

    @Override
    public void deleteImagesAfterCommit(Collection<String> imageUrls) {
        List<String> urls = imageUrls.stream().filter(Objects::nonNull).toList();
        if (urls.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            urls.forEach(this::deleteImageFiles);
            return;
        }
        // Erst nach dem Commit: schlägt das Löschen in der Datenbank fehl,
        // dürfen die Fotos nicht schon weg sein.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                urls.forEach(ItemServiceImpl.this::deleteImageFiles);
            }
        });
    }

    /** Entfernt das Originalfoto und seine Vorschaubilder. Fehler dabei sind unkritisch. */
    private void deleteImageFiles(String imageUrl) {
        try {
            String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
            Path baseDir = Paths.get(uploadDir).normalize().toAbsolutePath();
            Path original = baseDir.resolve(filename).normalize();
            if (filename.isBlank() || !baseDir.equals(original.getParent())) {
                return;
            }
            deleteQuietly(original);
            for (ImageSize size : ImageSize.values()) {
                deleteQuietly(baseDir.resolve(VARIANT_DIRECTORY)
                        .resolve(size.name().toLowerCase(Locale.ROOT) + "-" + filename + ".jpg"));
            }
        } catch (RuntimeException e) {
            log.warn("Could not remove image files for {}: {}", imageUrl, e.getMessage());
        }
    }

    private void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // liegengebliebene Temp-Datei ist harmlos
        }
    }

    @Override
    public String storeImage(MultipartFile imageFile) {
        return saveImage(imageFile);
    }

    private String saveImage(MultipartFile imageFile) {
        String contentType = imageFile.getContentType();
        String extension = contentType != null ? ALLOWED_IMAGE_TYPES.get(contentType) : null;
        if (extension == null) {
            throw new IllegalArgumentException("Only JPEG, PNG, WEBP or GIF images are allowed");
        }
        if (imageFile.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image must not be larger than 10 MB");
        }

        File directory = new File(uploadDir);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Could not create upload directory");
        }

        String uniqueFileName = UUID.randomUUID() + extension;

        File destinationFile = new File(directory, uniqueFileName);

        try {
            imageFile.transferTo(destinationFile);
        } catch (IOException e) {
            throw new IllegalStateException("Error saving image: " + e.getMessage());
        }

        // Varianten gleich beim Upload erzeugen, damit der erste Betrachter nicht wartet.
        for (ImageSize size : ImageSize.values()) {
            variantOf(destinationFile.toPath().toAbsolutePath().normalize(), size);
        }

        return "/api/items/images/" + uniqueFileName;
    }
}
