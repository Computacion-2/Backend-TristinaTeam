package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record CareUpdateCreateRequest(
        @NotNull @Positive Long bookingId,
        @NotBlank @Size(max = 1200) String updateMessage) {}
