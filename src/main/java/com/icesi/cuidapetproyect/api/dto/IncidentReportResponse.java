package com.icesi.cuidapetproyect.api.dto;

import java.time.LocalDateTime;

public record IncidentReportResponse(Long id, Long reporterId, Long reportedUserId, Long requestId, Long bookingId,
        String reportReason, String reportDetails, String evidenceUrl, String reportStatus, String resolutionNotes,
        Long resolvedById, LocalDateTime createdAt, LocalDateTime resolvedAt) {}
