package com.archiv.ArchivMgnSystem.services;

import com.archiv.ArchivMgnSystem.dtos.ItemDTO;
import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.enums.ImageSize;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ItemService {
    Response saveItem(ItemDTO itemDTO, MultipartFile imageFile, MultipartFile locationImageFile);

    Response updateItem(ItemDTO itemDTO, MultipartFile imageFile, MultipartFile locationImageFile);

    Response getAllItems(int page, int size, String filter, Long categoryId);

    Response getItemsById(Long id);

    Response deleteItem(Long id);

    Resource loadImage(String filename, ImageSize size);

}
