package com.icesi.cuidapetproyect.controller;

import org.springframework.dao.DataAccessException;

final class MvcMessages {

    private MvcMessages() {
    }

    // Los servicios lanzan RuntimeException con mensajes de negocio pensados para el usuario.
    // Los errores de persistencia no deben exponer SQL ni datos de conexión.
    static String errorMessage(RuntimeException ex) {
        if (ex instanceof DataAccessException || ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "No fue posible completar la operación. Inténtalo de nuevo.";
        }
        return ex.getMessage();
    }
}
