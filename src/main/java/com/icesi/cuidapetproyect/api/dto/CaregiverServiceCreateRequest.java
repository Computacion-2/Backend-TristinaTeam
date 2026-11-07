package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CaregiverServiceCreateRequest(@NotNull @Positive Long caregiverId, @NotNull @Positive Long serviceTypeId,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal baseRate) {}
