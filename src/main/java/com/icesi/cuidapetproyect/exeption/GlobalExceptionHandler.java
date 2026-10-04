package com.icesi.cuidapetproyect.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Cuando los datos enviados no son válidos.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(
            IllegalArgumentException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    // Cuando el registro solicitado no existe.
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(
            NoSuchElementException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    // Cuando una operación incumple una regla de negocio.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(
            IllegalStateException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    // Maneja las RuntimeException que lanzan nuestros servicios.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> runtimeException(
            RuntimeException ex) {

        String message = ex.getMessage();

        // Evitamos devolver mensajes internos si no hay una descripción.
        if (message == null || message.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor"));
        }

        String lowerMessage = message.toLowerCase();

        // Registros que no existen.
        if (lowerMessage.contains("no encontrado")
                || lowerMessage.contains("no encontrada")
                || lowerMessage.contains("not found")
                || lowerMessage.contains("does not exist")) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", message));
        }

        // Operaciones que incumplen las reglas de negocio.
        if (lowerMessage.contains("último")
                || lowerMessage.contains("ultimo")
                || lowerMessage.contains("al menos")
                || lowerMessage.contains("ya tiene")
                || lowerMessage.contains("ya existe")
                || lowerMessage.contains("duplicado")) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("error", message));
        }

        // Otras RuntimeException no identificadas.
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error interno del servidor"));
    }
}