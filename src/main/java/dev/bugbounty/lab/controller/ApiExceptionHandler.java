package dev.bugbounty.lab.controller;

import dev.bugbounty.lab.service.InvalidCredentialsException;
import dev.bugbounty.lab.service.InvalidRequestException;
import dev.bugbounty.lab.service.LabConflictException;
import dev.bugbounty.lab.service.LabUnauthorizedException;
import dev.bugbounty.lab.service.LabUnavailableException;
import dev.bugbounty.lab.service.ResourceNotFoundException;
import dev.bugbounty.lab.service.TooManyAttemptsException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<Map<String, String>> invalidCredentials(InvalidCredentialsException error) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    ResponseEntity<Map<String, String>> tooManyAttempts() {
        return ResponseEntity.status(429).body(Map.of("error", "Too many attempts"));
    }

    @ExceptionHandler(InvalidRequestException.class)
    ResponseEntity<Map<String, String>> invalidRequest(InvalidRequestException error) {
        return ResponseEntity.badRequest().body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Not found"));
    }

    @ExceptionHandler(LabConflictException.class)
    ResponseEntity<Map<String, String>> conflict(LabConflictException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler(LabUnauthorizedException.class)
    ResponseEntity<Map<String, String>> unauthorized(LabUnauthorizedException error) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler(LabUnavailableException.class)
    ResponseEntity<Map<String, String>> unavailable(LabUnavailableException error) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", error.getMessage()));
    }
}
