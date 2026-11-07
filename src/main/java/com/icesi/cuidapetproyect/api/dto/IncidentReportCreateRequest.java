package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record IncidentReportCreateRequest(
        @NotNull @Positive Long reporterId,
        @NotNull @Positive Long reportedUserId,
        @NotNull @Positive Long requestId,
        @Positive Long bookingId,
        @NotBlank @Size(max = 200) String reportReason,
        @NotBlank @Size(max = 2000) String reportDetails,
        @Size(max = 500) @Pattern(regexp = "https?://[^\\s]+") String evidenceUrl) {}
