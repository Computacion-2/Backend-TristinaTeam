package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record ServiceTypeUpdateRequest(
        @NotBlank @Size(max = 100) String serviceName,
        @Size(max = 400) String description,
        @NotBlank @Pattern(regexp = "Y|N") String activeFlag) {
}
