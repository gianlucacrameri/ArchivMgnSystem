package com.archiv.ArchivMgnSystem.services.impl;


import com.archiv.ArchivMgnSystem.dtos.ItemDTO;
import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.enums.ImageSize;
import com.archiv.ArchivMgnSystem.enums.StockPrecision;
import com.archiv.ArchivMgnSystem.exceptions.NotFoundException;
import com.archiv.ArchivMgnSystem.models.Category;
import com.archiv.ArchivMgnSystem.models.Item;
import com.archiv.ArchivMgnSystem.repositories.CategoryRepository;
import com.archiv.ArchivMgnSystem.repositories.ItemRepository;
import com.archiv.ArchivMgnSystem.repositories.ReservationRepository;
import com.archiv.ArchivMgnSystem.repositories.TransactionRepository;
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

        if (imageFile != null && !imageFile.isEmpty()) {
            itemToSave.setImageUrl(saveImage(imageFile));
        }
        if (locationImageFile != null && !locationImageFile.isEmpty()) {
            itemToSave.setLocationImageUrl(saveImage(locationImageFile));
        }

        // Artikelnummer wird nicht vom Admin vergeben, sondern fortlaufend ab 0
        // vergeben - unabhängig von der technischen DB-ID, damit fehlgeschlagene
        // Inserts oder gelöschte Items keine Lücken/Sprünge verursachen.
        int nextArticleNumber = itemRepository.findHighestArticleNumber() + 1;
        itemToSave.setSku(String.format("ART-%05d", nextArticleNumber));
        itemRepository.save(itemToSave);

        return Response.builder()
                .message("Item successfully saved")
                .build();
    }

    @Override
    public Response updateItem(ItemDTO itemDTO, MultipartFile imageFile, MultipartFile locationImageFile) {
        Item existingItem = itemRepository.findById(itemDTO.getItemId())
                .orElseThrow(() -> new NotFoundException("Item not found"));

        if (imageFile != null && !imageFile.isEmpty()) {
            existingItem.setImageUrl(saveImage(imageFile));
        }
        if (locationImageFile != null && !locationImageFile.isEmpty()) {
            existingItem.setLocationImageUrl(saveImage(locationImageFile));
        }
        if (itemDTO.getCategoryId() > 0) {
            Category category = categoryRepository.findById(itemDTO.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            existingItem.setCategory(category);
        }

        if (itemDTO.getName() != null && !itemDTO.getName().isBlank()) {
            existingItem.setName(itemDTO.getName());
        }

        if (itemDTO.getDescription() != null && !itemDTO.getDescription().isBlank()) {
            existingItem.setDescription(itemDTO.getDescription());
        }

        if (itemDTO.getPlace() != null && !itemDTO.getPlace().isBlank()) {
            existingItem.setPlace(itemDTO.getPlace());
        }

        if (itemDTO.getStockQuantity() != null && itemDTO.getStockQuantity() >= 0) {
            existingItem.setStockQuantity(itemDTO.getStockQuantity());
        }

        if (itemDTO.getStockPrecision() != null) {
            existingItem.setStockPrecision(itemDTO.getStockPrecision());
        }

        itemRepository.save(existingItem);

        return Response.builder()
                .status(200)
                .message("Item updated successfully")
                .build();

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
        itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found"));

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
