package com.archiv.ArchivMgnSystem.services;

import com.archiv.ArchivMgnSystem.dtos.ItemDTO;
import com.archiv.ArchivMgnSystem.dtos.Response;
import org.springframework.web.multipart.MultipartFile;

public interface ItemService {
    Response saveItem(ItemDTO itemDTO, MultipartFile imageFile);

    Response updateItem(ItemDTO itemDTO, MultipartFile imageFile);

    Response getAllItems();

    Response getItemsById(Long id);

    Response deleteItem(Long id);

    Response searchItem(String input);

}
