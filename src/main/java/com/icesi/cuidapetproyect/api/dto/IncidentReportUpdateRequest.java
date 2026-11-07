package com.icesi.cuidapetproyect.api.dto;

import jakarta.validation.constraints.*;

public record IncidentReportUpdateRequest(
        @NotBlank @Size(max = 200) String reportReason,
        @NotBlank @Size(max = 2000) String reportDetails,
        @Size(max = 500) @Pattern(regexp = "https?://[^\\s]+") String evidenceUrl,
        @NotBlank @Pattern(regexp = "OPEN|IN_REVIEW|RESOLVED|DISMISSED") String reportStatus,
        @Size(max = 1500) String resolutionNotes) {}
