package com.icesi.cuidapetproyect.api.dto;

import java.time.LocalDate;

public record CaregiverAvailabilityResponse(Long id, Long caregiverId, Integer dayOfWeek, String startTime, String endTime,
        LocalDate validFrom, LocalDate validUntil, String activeFlag) {}
