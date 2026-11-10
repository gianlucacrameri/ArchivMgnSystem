package com.archiv.ArchivMgnSystem.security;

import java.nio.charset.StandardCharsets;

/** Mindestanforderungen an Passwörter - gilt bei der Registrierung und beim Ändern. */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    /** BCrypt verarbeitet höchstens 72 Bytes. */
    public static final int MAX_BYTES = 72;

    public static final String DESCRIPTION =
            "Das Passwort braucht mindestens " + MIN_LENGTH + " Zeichen, davon mindestens einen Buchstaben und eine Zahl";

    private PasswordPolicy() {
    }

    public static boolean isTooLong(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES;
    }

    public static void validate(String password) {
        if (password == null
                || password.length() < MIN_LENGTH
                || password.chars().noneMatch(Character::isLetter)
                || password.chars().noneMatch(Character::isDigit)) {
            throw new IllegalArgumentException(DESCRIPTION);
        }
        if (isTooLong(password)) {
            throw new IllegalArgumentException("Das Passwort ist zu lang");
        }
    }
}
