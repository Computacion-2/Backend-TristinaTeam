package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.math.BigDecimal;

public record PetCreateRequest(
        @NotNull @Positive Long ownerId,
        @NotNull @Positive Long speciesId,
        @NotBlank @Size(max = 100) String petName,
        @Size(max = 100) String breed,
        @PastOrPresent LocalDate birthDate,
        @Pattern(regexp = "FEMALE|MALE|UNKNOWN") String sex,
        @DecimalMin(value = "0", inclusive = false) @Digits(integer = 4, fraction = 2) BigDecimal weightKg,
        @Size(max = 500) String temperament,
        @Size(max = 1000) String careInstructions,
        @Size(max = 1000) String medicalAlerts) {
}
