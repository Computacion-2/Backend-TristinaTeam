package com.icesi.cuidapetproyect.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private com.icesi.cuidapetproyect.exception.GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new com.icesi.cuidapetproyect.exception.GlobalExceptionHandler();
    }

    // 1. Datos inválidos: HTTP 400.
    @Test
    void badRequestTest() {

        ResponseEntity<Map<String, String>> response =
                handler.badRequest(
                        new IllegalArgumentException("Datos inválidos")
                );

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Datos inválidos",
                response.getBody().get("error"));
    }

    // 2. Registro inexistente: HTTP 404.
    @Test
    void notFoundTest() {

        ResponseEntity<Map<String, String>> response =
                handler.notFound(
                        new NoSuchElementException("Usuario no encontrado")
                );

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Usuario no encontrado",
                response.getBody().get("error"));
    }

    // 3. Regla de negocio incumplida: HTTP 409.
    @Test
    void conflictTest() {

        ResponseEntity<Map<String, String>> response =
                handler.conflict(
                        new IllegalStateException(
                                "No se puede retirar el último rol"
                        )
                );

        assertEquals(409, response.getStatusCode().value());
        assertEquals(
                "No se puede retirar el último rol",
                response.getBody().get("error")
        );
    }

    // 4. RuntimeException de un registro inexistente: HTTP 404.
    @Test
    void runtimeNotFoundTest() {

        ResponseEntity<Map<String, String>> response =
                handler.runtimeException(
                        new RuntimeException("Rol no encontrado")
                );

        assertEquals(404, response.getStatusCode().value());
    }

    // 5. RuntimeException por una regla de negocio: HTTP 409.
    @Test
    void runtimeConflictTest() {

        ResponseEntity<Map<String, String>> response =
                handler.runtimeException(
                        new RuntimeException(
                                "El usuario debe tener al menos un rol"
                        )
                );

        assertEquals(409, response.getStatusCode().value());
    }

    // 6. RuntimeException sin mensaje: HTTP 500.
    @Test
    void runtimeWithoutMessageTest() {

        ResponseEntity<Map<String, String>> response =
                handler.runtimeException(
                        new RuntimeException()
                );

        assertEquals(500, response.getStatusCode().value());
        assertEquals(
                "Error interno del servidor",
                response.getBody().get("error")
        );
    }

    // 7. RuntimeException desconocida: HTTP 500.
    @Test
    void runtimeUnknownTest() {

        ResponseEntity<Map<String, String>> response =
                handler.runtimeException(
                        new RuntimeException("Error inesperado")
                );

        assertEquals(500, response.getStatusCode().value());
        assertEquals(
                "Error interno del servidor",
                response.getBody().get("error")
        );
    }
    // 8. RuntimeException con mensaje vacío.
    @Test
    void runtimeEmptyMessageTest() {

        ResponseEntity<Map<String, String>> response =
                handler.runtimeException(
                        new RuntimeException("")
                );

        assertEquals(500, response.getStatusCode().value());
        assertEquals(
                "Error interno del servidor",
                response.getBody().get("error")
        );
    }

    // 9. Registro inexistente con mensaje en inglés.
    @Test
    void runtimeNotFoundEnglishTest() {

        ResponseEntity<Map<String, String>> response =
                handler.runtimeException(
                        new RuntimeException("User not found")
                );

        assertEquals(404, response.getStatusCode().value());
    }

    // 10. Registro duplicado.
    @Test
    void runtimeDuplicateTest() {

        ResponseEntity<Map<String, String>> response =
                handler.runtimeException(
                        new RuntimeException("El rol ya existe")
                );

        assertEquals(409, response.getStatusCode().value());
    }
}
