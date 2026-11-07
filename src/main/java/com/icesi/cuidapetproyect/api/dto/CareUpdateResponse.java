package com.icesi.cuidapetproyect.api.dto;

import java.time.LocalDateTime;

public record CareUpdateResponse(Long id, Long bookingId, Long caregiverId, String updateMessage,
        LocalDateTime createdAt) {}
