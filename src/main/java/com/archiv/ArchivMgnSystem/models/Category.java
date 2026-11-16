package com.archiv.ArchivMgnSystem.models;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "categories")
@Data
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    /**
     * Material dieser Kategorie kann verderben und darf ein Ablaufdatum haben.
     * Leer nur bei Kategorien aus älteren Versionen, bis der Startwert gesetzt ist.
     */
    private Boolean perishable;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    private List<Item> items;

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}