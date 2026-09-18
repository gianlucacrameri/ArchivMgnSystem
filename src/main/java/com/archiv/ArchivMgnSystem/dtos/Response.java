package com.archiv.ArchivMgnSystem.dtos;

import com.archiv.ArchivMgnSystem.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response {

    //Generics
    private int  status;
    private String message;
    //for login
    private String token;
    private UserRole role;
    private String expirationTime;

    //for pagination
    private Integer totalPages;
    private Long totalElements;

    //data output optionals
    private UserDTO user;
    private List<UserDTO> users;

    private PlaceDTO place;
    private List<PlaceDTO> places;

    private CategoryDTO category;
    private List<CategoryDTO> categories;

    private ItemDTO item;
    private List<ItemDTO> items;

    private TransactionDTO transaction;
    private List<TransactionDTO> transactions;

    private EventDTO event;
    private List<EventDTO> events;

    private final LocalDateTime timestamp = LocalDateTime.now();
}
