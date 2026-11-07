package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record BookingStatusUpdateRequest(@NotBlank @Size(max = 80) String statusName) {}
