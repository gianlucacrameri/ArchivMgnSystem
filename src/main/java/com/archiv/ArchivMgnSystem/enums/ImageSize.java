package com.archiv.ArchivMgnSystem.enums;

/**
 * Verkleinerte Varianten eines Item-Fotos. Handyfotos haben mehrere MB -
 * für Kacheln und Listen reicht ein Bruchteil davon.
 */
public enum ImageSize {
    THUMB(480, 0.8),
    MEDIUM(1600, 0.85);

    private final int maxEdge;
    private final double quality;

    ImageSize(int maxEdge, double quality) {
        this.maxEdge = maxEdge;
        this.quality = quality;
    }

    public int getMaxEdge() {
        return maxEdge;
    }

    public double getQuality() {
        return quality;
    }

    /** Unbekannte oder fehlende Angabe bedeutet: Originalbild. */
    public static ImageSize fromParam(String value) {
        if (value == null) {
            return null;
        }
        for (ImageSize size : values()) {
            if (size.name().equalsIgnoreCase(value)) {
                return size;
            }
        }
        return null;
    }
}
