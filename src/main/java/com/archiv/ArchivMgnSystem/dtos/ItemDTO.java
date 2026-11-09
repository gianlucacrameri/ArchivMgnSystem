package com.archiv.ArchivMgnSystem.dtos;


import com.archiv.ArchivMgnSystem.enums.StockPrecision;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemDTO {


    private long id;

    private long categoryId;
    private long placeId;
    private long itemId;


    private String name;


    private String sku;

    private String place;


    private Integer stockQuantity;

    private StockPrecision stockPrecision;


    private LocalDateTime expirationDate;
    private String description;
    private String imageUrl;
    private String locationImageUrl;
    private LocalDateTime createdAt;



}
