package com.archiv.ArchivMgnSystem.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Fachlicher Fehler mit eigenem HTTP-Status und einer Meldung, die der Benutzer sehen darf. */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
