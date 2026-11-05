package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record RolePermissionRequest(
        @NotNull @Positive Long roleId, @NotNull @Positive Long permissionId) {
}
