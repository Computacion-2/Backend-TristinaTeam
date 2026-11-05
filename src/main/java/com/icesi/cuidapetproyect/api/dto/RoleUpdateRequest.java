package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record RoleUpdateRequest(
        @NotBlank @Size(max = 60) String roleName,
        @Size(max = 300) String description,
        @NotBlank @Pattern(regexp = "Y|N") String activeFlag) {
}
