package catan.catanSpring.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GestorErrors {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity> validacioCos(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError camp : e.getBindingResult().getFieldErrors()) {
            errors.put(camp.getField(), camp.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }
}