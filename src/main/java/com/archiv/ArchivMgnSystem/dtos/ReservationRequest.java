package com.archiv.ArchivMgnSystem.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationRequest {

    @NotNull(message = "Item ID is required")
    private Long itemId;

    @Positive(message = "Quantity must be greater than 0")
    private Integer quantity;
}
