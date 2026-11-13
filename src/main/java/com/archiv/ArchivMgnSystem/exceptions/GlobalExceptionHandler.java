package com.archiv.ArchivMgnSystem.exceptions;

import com.archiv.ArchivMgnSystem.dtos.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response> handleAllException(Exception e) {
        // Framework-Fehler wie "Parameter fehlt" oder "Methode nicht erlaubt"
        // bringen ihren eigenen (4xx-)Status mit.
        if (e instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            return respond(status, status.is4xxClientError() ? "Ungültige Anfrage" : "Unerwarteter Fehler");
        }

        // Details (SQL, Tabellen-/Constraint-Namen, Stacktrace) gehören ins
        // Server-Log, nicht in die Antwort an den Browser.
        log.error("Unhandled exception", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Unerwarteter Fehler. Bitte später erneut versuchen.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Response> handleAccessDeniedException(AccessDeniedException e) {
        return respond(HttpStatus.FORBIDDEN, "Keine Berechtigung für diese Aktion");
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Response> handleNotFoundException(NotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialException.class)
    public ResponseEntity<Response> handleInvalidCredentialException(InvalidCredentialException e) {
        return respond(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Response> handleApiException(ApiException e) {
        return respond(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(NameValueRequiredException.class)
    public ResponseEntity<Response> handleNameValueRequiredException(NameValueRequiredException e) {
        return respond(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Response> handleBadRequest(RuntimeException e) {
        return respond(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return respond(HttpStatus.BAD_REQUEST, message.isBlank() ? "Ungültige Eingabe" : message);
    }

    // Falscher Wert in einem Parameter (z.B. unbekannte Auswahl) oder eine
    // Anfrage ohne die erwartete Datei: Fehler des Aufrufers, kein Serverfehler.
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MultipartException.class})
    public ResponseEntity<Response> handleBadParameter(Exception e) {
        return respond(HttpStatus.BAD_REQUEST, "Ungültige Anfrage");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Response> handleUnreadableBody(HttpMessageNotReadableException e) {
        return respond(HttpStatus.BAD_REQUEST, "Ungültige Anfrage");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Response> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMostSpecificCause().getMessage());
        return respond(HttpStatus.CONFLICT, "Der Eintrag existiert bereits oder wird noch verwendet");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Response> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return respond(HttpStatus.PAYLOAD_TOO_LARGE, "Datei ist zu gross (maximal 10 MB)");
    }

    private ResponseEntity<Response> respond(HttpStatusCode status, String message) {
        Response response = Response.builder()
                .status(status.value())
                .message(message)
                .build();
        return new ResponseEntity<>(response, status);
    }
}
