package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record PermissionCreateRequest(
        @NotBlank @Size(max = 100) String permissionCode,
        @NotBlank @Size(max = 120) String permissionName,
        @Size(max = 300) String description) {
}
