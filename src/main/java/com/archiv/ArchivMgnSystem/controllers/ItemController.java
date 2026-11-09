package com.archiv.ArchivMgnSystem.controllers;

import com.archiv.ArchivMgnSystem.dtos.ItemDTO;
import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.services.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<Response> saveProduct(
            @RequestParam("imageFile")MultipartFile imageFile,
            @RequestParam("name")String name,
            @RequestParam("sku")String sku,
            @RequestParam("stockQuantity")Integer stockQuantity,
            @RequestParam("categoryId")Long categoryId,
            @RequestParam("place")String place,
            @RequestParam(value = "description", required = false)String description
            ) {
        ItemDTO itemDTO = new ItemDTO();
        itemDTO.setName(name);
        itemDTO.setSku(sku);
        itemDTO.setPlace(place);
        itemDTO.setDescription(description);
        itemDTO.setCategoryId(categoryId);
        itemDTO.setStockQuantity(stockQuantity);
        itemDTO.setCreatedAt(LocalDateTime.now());

        return ResponseEntity.ok(itemService.saveItem(itemDTO, imageFile));
    }

    @PutMapping("/update")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<Response> updateItem(
            @RequestParam(value = "imageFile", required = false)MultipartFile imageFile,
            @RequestParam(value = "name", required = false)String name,
            @RequestParam(value = "sku", required = false)String sku,
            @RequestParam(value = "stockQuantity", required = false)Integer stockQuantity,
            @RequestParam(value = "categoryId", required = false)Long categoryId,
            @RequestParam("itemId")Long itemId,
            @RequestParam(value = "place", required = false)String place,
            @RequestParam(value = "description", required = false)String description
            ) {
        ItemDTO itemDTO = new ItemDTO();
        itemDTO.setName(name);
        itemDTO.setSku(sku);
        itemDTO.setPlace(place);
        itemDTO.setItemId(itemId);
        itemDTO.setDescription(description);
        itemDTO.setCategoryId(categoryId);
        itemDTO.setStockQuantity(stockQuantity);

        return ResponseEntity.ok(itemService.updateItem(itemDTO, imageFile));
    }

    @GetMapping("/all")
    public ResponseEntity<Response> getAllItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(itemService.getAllItems(page, size, filter, categoryId));
    }

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        Resource resource = itemService.loadImage(filename);

        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            String probed = Files.probeContentType(resource.getFile().toPath());
            if (probed != null) {
                contentType = MediaType.parseMediaType(probed);
            }
        } catch (IOException ignored) {
            // fall back to octet-stream
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getItemById(@PathVariable Long id){
        return ResponseEntity.ok(itemService.getItemsById(id));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<Response> deleteItem(@PathVariable Long id){
        return ResponseEntity.ok(itemService.deleteItem(id));
    }
}
