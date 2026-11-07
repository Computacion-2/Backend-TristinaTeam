package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CareApplicationUpdateRequest(
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal proposedRate,
        @Size(max = 1000) String applicationMessage,
        @NotBlank @Pattern(regexp = "PENDING|REJECTED|WITHDRAWN") String applicationStatus,
        @Size(max = 500) String responseReason) {}
