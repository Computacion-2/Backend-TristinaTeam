package com.icesi.cuidapetproyect.controller;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RoleForm {
    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(max = 60, message = "El nombre admite hasta 60 caracteres")
    @Pattern(regexp = "\\s*[A-Za-z][A-Za-z0-9_]*\\s*",
            message = "El nombre debe empezar por una letra y solo admite letras, números y guion bajo")
    private String roleName;

    @Size(max = 300, message = "La descripción admite hasta 300 caracteres")
    private String description;

    @NotBlank(message = "El estado del rol es obligatorio")
    @Pattern(regexp = "Y|N", message = "Estado del rol inválido")
    private String activeFlag = "Y";

    private List<Long> permissionIds = new ArrayList<>();
}
