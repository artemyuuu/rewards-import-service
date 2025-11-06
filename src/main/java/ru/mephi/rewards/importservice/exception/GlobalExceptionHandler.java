package ru.mephi.rewards.importservice.exception;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.mephi.rewards.importservice.service.model.ErrorResponse;

import java.time.OffsetDateTime;
import org.slf4j.Logger;


@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> internal(Exception ex) {
        log.error("Unexpected error", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    @ExceptionHandler(InvalidFileStructureException.class)
    public ResponseEntity<Object> handleInvalidStructure(InvalidFileStructureException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UnsupportedFileFormatException.class)
    public ResponseEntity<Object> handleUnsupportedFormat(UnsupportedFileFormatException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ResponseEntity<Object> buildResponse(HttpStatus httpStatus, String message) {
        ErrorResponse body = new ErrorResponse(OffsetDateTime.now(), httpStatus.value(),
                httpStatus.getReasonPhrase(), message);
        return ResponseEntity.status(httpStatus)
                .body(body);
    }
}
