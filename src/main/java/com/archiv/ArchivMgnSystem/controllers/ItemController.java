package com.archiv.ArchivMgnSystem.controllers;

import com.archiv.ArchivMgnSystem.dtos.ItemDTO;
import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.services.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping("/add")
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
    public ResponseEntity<Response> getAllItems(){
        return ResponseEntity.ok(itemService.getAllItems());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getItemById(@PathVariable Long id){
        return ResponseEntity.ok(itemService.getItemsById(id));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Response> deleteItem(@PathVariable Long id){
        return ResponseEntity.ok(itemService.deleteItem(id));
    }

    @GetMapping("/search")
    public ResponseEntity<Response> searchItem(@RequestParam String input){
        return ResponseEntity.ok(itemService.searchItem(input));
    }
}
