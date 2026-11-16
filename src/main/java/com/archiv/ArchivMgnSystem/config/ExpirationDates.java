package com.archiv.ArchivMgnSystem.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Ablaufdaten kommen als Tag (JJJJ-MM-TT) vom Browser; eine Uhrzeit gibt es dabei nicht. */
public final class ExpirationDates {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private ExpirationDates() {
    }

    /** null oder leer ergibt null; ein ungültiges Datum wird als Eingabefehler gemeldet. */
    public static LocalDateTime parse(String value) {
        LocalDate date = parseDate(value);
        return date == null ? null : date.atStartOfDay();
    }

    public static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(value.trim());
            if (date.getYear() < 2000 || date.getYear() > 2100) {
                throw new IllegalArgumentException("Bitte ein Ablaufdatum zwischen 2000 und 2100 angeben");
            }
            return date;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ungültiges Ablaufdatum");
        }
    }

    public static String format(LocalDate date) {
        return date == null ? "–" : date.format(DISPLAY);
    }

    public static String format(LocalDateTime date) {
        return date == null ? "–" : date.toLocalDate().format(DISPLAY);
    }
}
