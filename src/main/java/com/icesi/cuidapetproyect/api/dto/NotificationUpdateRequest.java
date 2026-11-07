package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

/** El destinatario solo cambia readFlag; ADMIN también puede corregir título y mensaje. */
public record NotificationUpdateRequest(
        @NotBlank @Pattern(regexp = "Y|N") String readFlag,
        @Size(min = 1, max = 180) String title,
        @Size(min = 1, max = 1000) String messageText) {}
