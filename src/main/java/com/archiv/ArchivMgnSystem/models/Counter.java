package com.archiv.ArchivMgnSystem.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Fortlaufender Zähler, der auch dann weiterzählt, wenn Einträge gelöscht werden. */
@Entity
@Table(name = "counters")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Counter {

    public static final String ARTICLE_NUMBER = "ARTICLE_NUMBER";

    @Id
    @Column(length = 50)
    private String name;

    @Column(nullable = false)
    private int nextValue;
}
