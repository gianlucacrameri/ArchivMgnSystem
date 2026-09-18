package com.archiv.ArchivMgnSystem.dtos;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Phonenumber is required")
    private String phoneNumber;

    // Die Rolle wird bewusst nicht vom Client übernommen (siehe UserServiceImpl.registerUser) -
    // Selbstregistrierung darf niemals ADMIN/MANAGER vergeben können.
}
