package com.icesi.cuidapetproyect.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CareBookingResponse(Long id, Long requestId, Long acceptedApplicationId, Long ownerId, Long caregiverId,
        BigDecimal agreedRate, String exactAddress, String bookingStatusCode, String ownerConfirmedFlag,
        String caregiverDoneFlag, String cancellationReason, LocalDateTime acceptedAt, LocalDateTime startedAt,
        LocalDateTime completedAt, LocalDateTime cancelledAt) {}
