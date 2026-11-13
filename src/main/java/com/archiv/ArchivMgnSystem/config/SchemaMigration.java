package com.archiv.ArchivMgnSystem.config;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Kleine Anpassungen an bestehenden Datenbanken, die Hibernate nicht selbst
 * vornimmt (es ergänzt Spalten, ändert aber nie den Typ einer vorhandenen).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaMigration implements ApplicationRunner {

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Ältere Installationen haben die Antragsart als festen Aufzählungstyp
        // ('ADD','USE') angelegt - die neue Art "PHOTO" passt dort nicht hinein.
        widenEnumColumn("transactions", "transaction_type");
    }

    private void widenEnumColumn(String table, String column) {
        try {
            List<?> types = entityManager.createNativeQuery(
                            "SELECT column_type FROM information_schema.columns "
                                    + "WHERE table_schema = DATABASE() AND table_name = :table AND column_name = :column")
                    .setParameter("table", table)
                    .setParameter("column", column)
                    .getResultList();
            if (!types.isEmpty() && String.valueOf(types.get(0)).toLowerCase().startsWith("enum")) {
                entityManager.createNativeQuery("ALTER TABLE " + table + " MODIFY " + column + " VARCHAR(20)").executeUpdate();
                log.info("Schema angepasst: {}.{} ist jetzt ein Textfeld", table, column);
            }
        } catch (RuntimeException e) {
            log.error("Schema-Anpassung für {}.{} fehlgeschlagen: {}", table, column, e.getMessage());
        }
    }
}
