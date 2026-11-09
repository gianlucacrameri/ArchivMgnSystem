package com.archiv.ArchivMgnSystem.services.impl;


import com.archiv.ArchivMgnSystem.dtos.ItemDTO;
import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.exceptions.NotFoundException;
import com.archiv.ArchivMgnSystem.models.Category;
import com.archiv.ArchivMgnSystem.models.Item;
import com.archiv.ArchivMgnSystem.repositories.CategoryRepository;
import com.archiv.ArchivMgnSystem.repositories.ItemRepository;
import com.archiv.ArchivMgnSystem.services.ItemService;
import com.archiv.ArchivMgnSystem.specification.ItemFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final ModelMapper modelMapper;
    private final CategoryRepository categoryRepository;

    @Value("${app.upload-dir:${user.dir}/uploads/items}")
    private String uploadDir;

    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

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
    public Response saveItem(ItemDTO itemDTO, MultipartFile imageFile) {

        Category category = categoryRepository.findById(itemDTO.getCategoryId())
                .orElseThrow(() ->new NotFoundException("Category not found"));

        //map dto to product entity
        Item itemToSave = Item.builder()
                .name(itemDTO.getName())
                .sku(itemDTO.getSku())
                .stockQuantity(itemDTO.getStockQuantity())
                .place(itemDTO.getPlace())
                .description(itemDTO.getDescription())
                .category(category)
                .createdAt(LocalDateTime.now())
                .build();

        if (imageFile != null && !imageFile.isEmpty()) {
            log.info("image file exists");
            String imagePath = saveImage(imageFile);
            itemToSave.setImageUrl(imagePath);
        }

        itemRepository.save(itemToSave);

        return Response.builder()
                .message("Item successfully saved")
                .build();
    }

    @Override
    public Response updateItem(ItemDTO itemDTO, MultipartFile imageFile) {
        Item existingItem = itemRepository.findById(itemDTO.getItemId())
                .orElseThrow(() -> new NotFoundException("Item not found"));

        if (imageFile != null && !imageFile.isEmpty()) {
            String imagePath = saveImage(imageFile);
            existingItem.setImageUrl(imagePath);
        }
        if (itemDTO.getCategoryId() > 0) {
            Category category = categoryRepository.findById(itemDTO.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            existingItem.setCategory(category);
        }

        if (itemDTO.getName() != null && !itemDTO.getName().isBlank()) {
            existingItem.setName(itemDTO.getName());
        }

        if (itemDTO.getSku() != null && !itemDTO.getSku().isBlank()) {
            existingItem.setSku(itemDTO.getSku());
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
    public Response deleteItem(Long id) {
        itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found"));
        itemRepository.deleteById(id);

        return Response.builder()
                .status(200)
                .message("Item deleted successfully")
                .build();

    }

    @Override
    public Resource loadImage(String filename) {
        try {
            Path baseDir = Paths.get(uploadDir).normalize().toAbsolutePath();
            Path filePath = baseDir.resolve(filename).normalize();

            if (!filePath.startsWith(baseDir)) {
                throw new IllegalArgumentException("Invalid file path");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("Image not found");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new NotFoundException("Image not found");
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

        return "/api/items/images/" + uniqueFileName;
    }
}
