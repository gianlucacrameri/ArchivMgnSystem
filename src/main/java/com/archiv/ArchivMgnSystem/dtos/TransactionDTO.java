package com.archiv.ArchivMgnSystem.dtos;


import com.archiv.ArchivMgnSystem.enums.PhotoTarget;
import com.archiv.ArchivMgnSystem.enums.TransactionStatus;
import com.archiv.ArchivMgnSystem.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class TransactionDTO {


    private long id;

    private Integer totalItems;

    @JsonIgnore
    private String password;


    private TransactionType transactionType;


    private TransactionStatus transactionStatus;

    private String note;
    private String description;
    private String proposedImageUrl;
    private PhotoTarget photoTarget;
    private String decidedByName;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;


    private ItemDTO item;


    private UserDTO user;


}
