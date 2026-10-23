package com.icesi.cuidapetproyect.controller;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class UserForm {
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Escribe un correo válido")
    @Size(max = 160, message = "El correo admite hasta 160 caracteres")
    private String email;

    @Size(max = 72, message = "La contraseña admite hasta 72 caracteres")
    private String password;

    @NotBlank
    @Pattern(regexp = "ACTIVE|INACTIVE|SUSPENDED|DELETED", message = "Estado de cuenta inválido")
    private String accountStatus = "ACTIVE";

    private List<Long> roleIds = new ArrayList<>();
}
