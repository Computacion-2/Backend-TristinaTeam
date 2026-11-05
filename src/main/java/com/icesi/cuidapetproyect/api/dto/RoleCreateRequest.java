package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record RoleCreateRequest(
        @NotBlank @Size(max = 60) String roleName,
        @Size(max = 300) String description,
        @NotEmpty List<@NotNull @Positive Long> permissionIds) {
}
