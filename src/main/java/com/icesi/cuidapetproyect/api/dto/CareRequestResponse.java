package com.icesi.cuidapetproyect.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CareRequestResponse(Long id, Long ownerId, Long petId, Long serviceTypeId,
        LocalDateTime startAt, LocalDateTime endAt, String city, String zoneName, String addressReference,
        String requestNotes, BigDecimal maxBudget, String requestStatus, String cancellationReason,
        LocalDateTime createdAt, LocalDateTime updatedAt) {}
