package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record CareBookingUpdateRequest(
        @NotBlank @Pattern(regexp = "SCHEDULED|IN_PROGRESS|PENDING_CONFIRMATION|COMPLETED|CANCELLED") String bookingStatusCode,
        @NotBlank @Size(max = 500) String exactAddress,
        @Size(max = 500) String cancellationReason) {}
