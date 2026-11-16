package com.archiv.ArchivMgnSystem.config;

import java.util.List;
import java.util.Locale;

/**
 * Erkennt am Namen, ob das Material einer Kategorie verderben kann (Essen,
 * Getränke usw.). Das ist nur der Vorschlag für neue und der Startwert für
 * bestehende Kategorien - ein Admin kann es pro Kategorie umstellen.
 */
public final class PerishableCategories {

    private static final List<String> KEYWORDS = List.of(
            "essen", "getränk", "getraenk", "trinken", "lebensmittel", "verpflegung", "nahrung", "speise",
            "snack", "süss", "suess", "proviant", "food", "drink", "verbrauch", "apotheke", "sanität", "sanitaet",
            "erste hilfe", "medizin");

    private PerishableCategories() {
    }

    public static boolean looksPerishable(String categoryName) {
        if (categoryName == null) {
            return false;
        }
        String name = categoryName.toLowerCase(Locale.ROOT);
        return KEYWORDS.stream().anyMatch(name::contains);
    }
}
