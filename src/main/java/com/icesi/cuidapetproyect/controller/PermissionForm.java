package com.icesi.cuidapetproyect.controller;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionForm {
    // Solo se usa al crear: el código es la autoridad de Spring Security y no se edita después.
    @Size(max = 100, message = "El código admite hasta 100 caracteres")
    @Pattern(regexp = "\\s*[A-Za-z][A-Za-z0-9_]*\\s*",
            message = "El código debe empezar por una letra y solo admite letras, números y guion bajo")
    private String permissionCode;

    @NotBlank(message = "El nombre del permiso es obligatorio")
    @Size(max = 120, message = "El nombre admite hasta 120 caracteres")
    private String permissionName;

    @Size(max = 300, message = "La descripción admite hasta 300 caracteres")
    private String description;
}
