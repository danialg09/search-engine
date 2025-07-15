package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import searchengine.dto.response.ErrorResponse;
import searchengine.exception.EntityNotFoundException;
import searchengine.exception.IndexingException;

@RestControllerAdvice
public class ExceptionHandlerController {

    @ExceptionHandler({IndexingException.class})
    public ResponseEntity<ErrorResponse> badRequest(IndexingException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(false, e.getMessage()));
    }

    @ExceptionHandler({EntityNotFoundException.class})
    public ResponseEntity<ErrorResponse> notFound(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(false, e.getMessage()));
    }
}
