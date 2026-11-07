package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record CareBookingCreateRequest(
        @NotNull @Positive Long applicationId,
        @NotBlank @Size(max = 500) String exactAddress) {}
