package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CaregiverServiceUpdateRequest(@NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal baseRate,
        @NotBlank @Pattern(regexp = "Y|N") String activeFlag) {}
