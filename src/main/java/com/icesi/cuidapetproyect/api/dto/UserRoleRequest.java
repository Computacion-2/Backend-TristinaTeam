package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record UserRoleRequest(
        @NotNull @Positive Long userId, @NotNull @Positive Long roleId) {
}
