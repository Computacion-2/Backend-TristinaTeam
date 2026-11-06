package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record ServiceTypeCreateRequest(
        @NotBlank @Size(max = 100) String serviceName,
        @Size(max = 400) String description) {
}
