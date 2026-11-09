package com.archiv.ArchivMgnSystem.models;


import com.archiv.ArchivMgnSystem.enums.StockPrecision;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "items")
@Data
@Builder
public class Item {

    @jakarta.persistence.Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true)
    @NotBlank(message = "Name is required")
    private String name;


    @Column(unique = true)
    private String sku;

    @NotBlank(message = "Place is required")
    private String place;

    @Min(value = 0, message = "Minimum quantity is 0")
    private Integer stockQuantity;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private StockPrecision stockPrecision = StockPrecision.EXACT;

    private LocalDateTime expirationDate;
    private String description;
    private String imageUrl;
    private String locationImageUrl;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Override
    public String toString() {
        return "Item{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", sku='" + sku + '\'' +
                ", stockQuantity=" + stockQuantity +
                ", stockPrecision=" + stockPrecision +
                ", expirationDate=" + expirationDate +
                ", description='" + description + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", createdAt=" + createdAt +
                ", place="  + place +
                '}';
    }
}
