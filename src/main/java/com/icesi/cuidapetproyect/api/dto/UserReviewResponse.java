package com.icesi.cuidapetproyect.api.dto;

import java.time.LocalDateTime;

public record UserReviewResponse(Long id, Long bookingId, Long reviewerId, Long reviewedUserId, Integer rating,
        String reviewComment, LocalDateTime createdAt) {}
