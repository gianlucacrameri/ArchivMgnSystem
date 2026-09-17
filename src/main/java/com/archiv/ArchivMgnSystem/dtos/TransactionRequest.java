package com.archiv.ArchivMgnSystem.dtos;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionRequest {

    @Positive(message = "Item ID is required")
    private Long itemId;

    @Positive(message = "Quantity is required")
    private Integer quantity;

    @Positive(message = "Place ID is required")
    private Long placeId;

    private String description;

    private String note;


}
