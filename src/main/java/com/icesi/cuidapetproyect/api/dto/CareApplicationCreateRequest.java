package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CareApplicationCreateRequest(
        @NotNull @Positive Long requestId,
        @NotNull @Positive Long caregiverId,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal proposedRate,
        @Size(max = 1000) String applicationMessage) {}
