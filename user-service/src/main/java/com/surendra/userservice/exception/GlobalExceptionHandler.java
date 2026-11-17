package com.surendra.userservice.exception;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });

        return errors;
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidRequestBody(
            HttpMessageNotReadableException ex) {

        Map<String, String> error = new HashMap<>();

        error.put("message", "Invalid request body");

        return error;
    }
    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex) {

        Map<String, String> error = new HashMap<>();

        error.put("message", ex.getMessage());

        return error;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleInvalidCredentials(
            InvalidCredentialsException ex) {

        Map<String, String> error = new HashMap<>();

        error.put("message", ex.getMessage());

        return error;
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleUserNotFound(
            UserNotFoundException ex) {

        Map<String, String> error = new HashMap<>();

        error.put("message", ex.getMessage());

        return error;
    }

}
