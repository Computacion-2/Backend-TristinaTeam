package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record PermissionUpdateRequest(
        @NotBlank @Size(max = 120) String permissionName,
        @Size(max = 300) String description) {
}
