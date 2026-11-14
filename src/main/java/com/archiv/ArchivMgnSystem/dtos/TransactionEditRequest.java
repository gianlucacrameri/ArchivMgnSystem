package com.archiv.ArchivMgnSystem.dtos;

import com.archiv.ArchivMgnSystem.enums.PhotoTarget;
import com.archiv.ArchivMgnSystem.enums.StockPrecision;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Anpassungen eines Admins an einem offenen Antrag, bevor er ihn freigibt.
 * Nicht gesetzte Felder bleiben unverändert.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionEditRequest {
    /** Menge (Bestandsanträge) bzw. Anfangsbestand (neues Material). */
    private Integer quantity;
    private PhotoTarget photoTarget;

    // nur bei Anträgen auf ein neues Material
    private String name;
    private String place;
    private Long categoryId;
    private StockPrecision stockPrecision;
    private String description;
}
