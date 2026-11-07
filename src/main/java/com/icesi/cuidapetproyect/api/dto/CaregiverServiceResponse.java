package com.icesi.cuidapetproyect.api.dto;

import java.math.BigDecimal;

public record CaregiverServiceResponse(Long caregiverId, Long serviceTypeId, BigDecimal baseRate, String activeFlag) {}
