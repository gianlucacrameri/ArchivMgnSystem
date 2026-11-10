package com.archiv.ArchivMgnSystem.models;


import com.archiv.ArchivMgnSystem.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
@Data
@Builder
public class User {

    @jakarta.persistence.Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    @NotBlank(message = "Name is required")
    private String username;

    @JsonIgnore
    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Email is required")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Phonenumber is required")
    @Column(unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    /**
     * Neue Konten müssen von einem Admin freigegeben werden, bevor sie sich
     * anmelden können. Bestehende Konten (Spalte wird nachträglich ergänzt)
     * gelten über den DB-Default als freigegeben.
     */
    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("1")
    private boolean approved = true;

    /**
     * Steckt in jedem ausgestellten Login-Token. Wird die Zahl erhöht
     * (Passwort geändert, "überall abmelden"), sind alle älteren Tokens ungültig.
     */
    @JsonIgnore
    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("0")
    private int tokenVersion = 0;

    /**
     * Gesetzt, wenn ein Admin ein vorläufiges Passwort vergeben hat. Die
     * Oberfläche fordert dann dazu auf, ein eigenes Passwort zu setzen.
     */
    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("0")
    private boolean passwordResetRequired = false;

    @OneToMany(mappedBy = "user")
    private List<Transaction> transactions;

    @Column(name = "created_at")
    private final LocalDateTime createdAt = LocalDateTime.now();


    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", transactions=" + transactions +
                ", createdAt=" + createdAt +
                '}';
    }
}
