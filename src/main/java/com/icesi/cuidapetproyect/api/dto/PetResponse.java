package com.icesi.cuidapetproyect.api.dto;

import java.time.*;
import java.math.BigDecimal;

public record PetResponse(
        Long id, Long ownerId, Long speciesId, String petName, String breed,
        LocalDate birthDate, String sex, BigDecimal weightKg, String temperament,
        String careInstructions, String medicalAlerts, String petStatus,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
