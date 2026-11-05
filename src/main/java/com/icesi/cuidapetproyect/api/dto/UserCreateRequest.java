package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record UserCreateRequest(
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotEmpty List<@NotNull @Positive Long> roleIds) {
}
