package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record BookingStatusCreateRequest(
        @NotBlank @Pattern(regexp = "SCHEDULED|IN_PROGRESS|PENDING_CONFIRMATION|COMPLETED|CANCELLED") String statusCode,
        @NotBlank @Size(max = 80) String statusName) {}
