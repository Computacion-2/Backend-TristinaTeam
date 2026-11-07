package com.icesi.cuidapetproyect.api.service;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.*;
import com.icesi.cuidapetproyect.api.security.*;
import com.icesi.cuidapetproyect.exception.*;
import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaregiverAvailabilityService {
    private final CaregiverAvailabilityRepository repository;
    private final CaregiverAvailabilityMapper mapper;
    private final ResourceAccess access;
    private final CaregiverAccess caregivers;

    public List<CaregiverAvailabilityResponse> findAll(Authentication auth) {
        return repository.findAll().stream()
                .filter(entity -> caregivers.canRead(entity.getCaregiver(), "Y".equals(entity.getActiveFlag()), auth))
                .map(mapper::entityToDto).toList();
    }

    public CaregiverAvailabilityResponse findById(Long id, Authentication auth) {
        var entity = require(id);
        caregivers.requireRead(entity.getCaregiver(), "Y".equals(entity.getActiveFlag()), auth);
        return mapper.entityToDto(entity);
    }

    @Transactional
    public CaregiverAvailabilityResponse create(CaregiverAvailabilityCreateRequest dto, Authentication auth) {
        access.requireOwnerOrAdmin(dto.caregiverId(), auth);
        var profile = caregivers.requireAvailable(dto.caregiverId());
        validatePeriod(dto.startTime(), dto.endTime(), dto.validFrom(), dto.validUntil());
        var entity = mapper.dtoToEntity(dto);
        entity.setCaregiver(profile);
        return mapper.entityToDto(repository.saveAndFlush(entity));
    }

    @Transactional
    public CaregiverAvailabilityResponse update(Long id, CaregiverAvailabilityUpdateRequest dto, Authentication auth) {
        var entity = require(id);
        access.requireOwnerOrAdmin(entity.getCaregiver().getId(), auth);
        if ("Y".equals(dto.activeFlag())) caregivers.requireAvailable(entity.getCaregiver().getId());
        validatePeriod(dto.startTime(), dto.endTime(), dto.validFrom(), dto.validUntil());
        mapper.dtoToEntity(dto, entity);
        return mapper.entityToDto(repository.saveAndFlush(entity));
    }

    @Transactional
    public void delete(Long id, Authentication auth) {
        var entity = require(id);
        access.requireOwnerOrAdmin(entity.getCaregiver().getId(), auth);
        repository.delete(entity);
        repository.flush();
    }

    private CaregiverAvailability require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Disponibilidad no encontrada"));
    }

    private void validatePeriod(String start, String end, LocalDate from, LocalDate until) {
        if (!LocalTime.parse(start).isBefore(LocalTime.parse(end))) {
            throw new IllegalArgumentException("La hora inicial debe ser anterior a la final");
        }
        if (from != null && until != null && until.isBefore(from)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial");
        }
    }
}
