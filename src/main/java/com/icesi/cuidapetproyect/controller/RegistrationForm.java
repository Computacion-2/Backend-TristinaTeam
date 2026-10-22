package com.icesi.cuidapetproyect.controller;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationForm {
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Escribe un correo válido")
    @Size(max = 160, message = "El correo admite hasta 160 caracteres")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    private String password;
}
