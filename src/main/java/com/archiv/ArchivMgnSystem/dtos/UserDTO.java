package com.archiv.ArchivMgnSystem.dtos;


import com.archiv.ArchivMgnSystem.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDTO {


    private Long id;


    private String username;


    // Darf vom Client gesendet werden (Passwort ändern), aber nie in einer
    // Antwort zurückgegeben werden - sonst leakt der Bcrypt-Hash im JSON.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;


    private String email;

    private String phoneNumber;


    private UserRole role;

    private List<TransactionDTO> transactions;

    private LocalDateTime createdAt;



}
