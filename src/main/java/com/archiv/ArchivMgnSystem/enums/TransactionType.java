package com.archiv.ArchivMgnSystem.enums;

public enum TransactionType {
    ADD, USE,
    /** Antrag, ein Foto des Materials oder Lagerorts durch ein neues zu ersetzen. */
    PHOTO,
    /** Antrag, ein neues Material ins Archiv aufzunehmen. */
    NEW,
    /** Antrag, ein Material aus dem Archiv zu löschen. */
    DELETE
}
