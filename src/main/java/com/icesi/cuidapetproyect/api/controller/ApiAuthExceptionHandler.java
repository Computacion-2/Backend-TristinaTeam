package com.icesi.cuidapetproyect.api.controller;

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice(assignableTypes = ApiAuthController.class)
@Order(0)
public class ApiAuthExceptionHandler {
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> invalidCredentials() {
        return ResponseEntity.status(401).header("WWW-Authenticate", "Bearer")
                .body(Map.of("error", "Credenciales incorrectas o cuenta no disponible"));
    }
}
