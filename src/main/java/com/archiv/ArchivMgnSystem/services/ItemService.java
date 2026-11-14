package com.archiv.ArchivMgnSystem.services;

import com.archiv.ArchivMgnSystem.dtos.ItemDTO;
import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.enums.ImageSize;
import com.archiv.ArchivMgnSystem.enums.StockPrecision;
import com.archiv.ArchivMgnSystem.models.Category;
import com.archiv.ArchivMgnSystem.models.Item;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;

public interface ItemService {
    Response saveItem(ItemDTO itemDTO, MultipartFile imageFile, MultipartFile locationImageFile);

    Response updateItem(ItemDTO itemDTO, MultipartFile imageFile, MultipartFile locationImageFile);

    Response getAllItems(int page, int size, String filter, Long categoryId);

    Response getItemsById(Long id);

    Response deleteItem(Long id);

    Resource loadImage(String filename, ImageSize size);

    /** Prüft und speichert ein hochgeladenes Foto und gibt dessen abrufbare Adresse zurück. */
    String storeImage(MultipartFile imageFile);

    /** Legt ein Material aus einem freigegebenen Antrag an; das Foto ist bereits gespeichert. */
    Item createFromProposal(String name, String place, int quantity, StockPrecision precision,
                            Category category, String description, String imageUrl, String requestedBy);

    /** Löscht die Fotodateien (samt Vorschaubildern), sobald die laufende Transaktion erfolgreich war. */
    void deleteImagesAfterCommit(Collection<String> imageUrls);

}
