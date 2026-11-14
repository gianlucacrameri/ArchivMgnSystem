package com.archiv.ArchivMgnSystem.models;


import com.archiv.ArchivMgnSystem.enums.PhotoTarget;
import com.archiv.ArchivMgnSystem.enums.StockPrecision;
import com.archiv.ArchivMgnSystem.enums.TransactionStatus;
import com.archiv.ArchivMgnSystem.enums.TransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "transactions")
@Data
@Builder
public class Transaction {

    @jakarta.persistence.Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;


    private Integer totalItems;


    // Als Textfeld gespeichert, damit neue Arten ohne Schemaänderung möglich sind.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    private TransactionStatus transactionStatus;

    private String note;
    private String description;

    /** Nur bei Foto-Anträgen: das vorgeschlagene Foto und welches es ersetzen soll. */
    private String proposedImageUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private PhotoTarget photoTarget;

    /**
     * Name des betroffenen Materials als Text: bei Anträgen auf ein neues
     * Material (das es noch nicht gibt) und auf Löschung (danach gibt es es
     * nicht mehr) der einzige Hinweis, worum es ging.
     */
    private String subjectName;

    // Nur bei Anträgen auf ein neues Material: die vorgeschlagenen Angaben.
    private String proposedPlace;
    private String proposedDescription;
    private Long proposedCategoryId;
    private String proposedCategoryName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private StockPrecision proposedStockPrecision;

    /** Wer den Antrag freigegeben oder abgelehnt hat (als Text, bleibt auch nach Löschen des Kontos). */
    private String decidedByName;

    private LocalDateTime updatedAt;
    private final LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", totalItems=" + totalItems +
                ", transactionType=" + transactionType +
                ", transactionStatus=" + transactionStatus +
                ", note='" + note + '\'' +
                ", description='" + description + '\'' +
                ", updatedAt=" + updatedAt +
                ", createdAt=" + createdAt +
                '}';
    }
}
