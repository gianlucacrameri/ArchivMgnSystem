package com.archiv.ArchivMgnSystem.services.impl;


import com.archiv.ArchivMgnSystem.dtos.ItemDTO;
import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.exceptions.NotFoundException;
import com.archiv.ArchivMgnSystem.models.Category;
import com.archiv.ArchivMgnSystem.models.Item;
import com.archiv.ArchivMgnSystem.repositories.CategoryRepository;
import com.archiv.ArchivMgnSystem.repositories.ItemRepository;
import com.archiv.ArchivMgnSystem.services.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final ModelMapper modelMapper;
    private final CategoryRepository categoryRepository;

    private static final String IMAGE_DIRECTORY = System.getProperty("user.dir") + "/item-image";


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

        if (itemDTO.getName() != null && !itemDTO.getName().isBlank()) {
            existingItem.setName(itemDTO.getName());
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
    public Response getAllItems() {
        List<Item> itemList = itemRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        List<ItemDTO> itemDTOList = modelMapper.map(itemList, new TypeToken<List<ItemDTO>>()
        {}.getType());


        return Response.builder()
                .status(200)
                .message("success")
                .items(itemDTOList)
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
    public Response searchItem(String input) {

        List<Item> items = itemRepository.findByNameContainingOrDescriptionContaining(input, input);

        if(items.isEmpty()) {
            throw new NotFoundException("Item not found");
        }

        List<ItemDTO> itemDTOList = modelMapper.map(items, new TypeToken<List<ItemDTO>>()
        {}.getType());
        return Response.builder()
                .status(200)
                .message("success")
                .items(itemDTOList)
                .build();

    }

    private String saveImage(MultipartFile imageFile) {
        if (!imageFile.getContentType().startsWith("image/") || imageFile.getSize() > 1024 * 1024 * 1024) {
            throw new IllegalArgumentException("Only image files under 1GB are allowed");
        }
        File directory = new File(IMAGE_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdir();
            log.info("directory was created");
        }
        // generate unique file name for the imaage
        String uniqueFileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();

        // get the absolute path

        String imagePath = IMAGE_DIRECTORY + uniqueFileName;

        try {
            File destinationFile = new File(imagePath);
            imageFile.transferTo(destinationFile); //we are writing the image to this folder
        } catch (IOException e) {
            throw new IllegalArgumentException("Error saving image: " + e.getMessage());
        }

        return imagePath;
    }
}
