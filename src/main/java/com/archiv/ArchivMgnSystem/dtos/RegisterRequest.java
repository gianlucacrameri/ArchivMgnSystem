package com.archiv.ArchivMgnSystem.dtos;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name ist zu lang")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Bitte eine gültige E-Mail-Adresse angeben")
    @Size(max = 200, message = "E-Mail ist zu lang")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Phonenumber is required")
    @Size(max = 40, message = "Telefonnummer ist zu lang")
    private String phoneNumber;

    // Die Rolle wird bewusst nicht vom Client übernommen (siehe UserServiceImpl.registerUser) -
    // Selbstregistrierung darf niemals ADMIN/MANAGER vergeben können.
}
