package com.icesi.cuidapetproyect.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CareApplicationResponse(Long id, Long requestId, Long caregiverId, BigDecimal proposedRate,
        String applicationMessage, String applicationStatus, String responseReason,
        LocalDateTime createdAt, LocalDateTime updatedAt) {}
