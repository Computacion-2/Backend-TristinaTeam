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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaregiverOfferingService {
    private final CaregiverServiceRepository repository;
    private final ServiceTypeRepository serviceTypes;
    private final CaregiverServiceMapper mapper;
    private final ResourceAccess access;
    private final CaregiverAccess caregivers;

    public List<CaregiverServiceResponse> findAll(Authentication auth) {
        return repository.findAll().stream()
                .filter(entity -> caregivers.canRead(entity.getCaregiver(), isActive(entity), auth))
                .map(mapper::entityToDto).toList();
    }

    public CaregiverServiceResponse findById(Long caregiverId, Long serviceTypeId, Authentication auth) {
        var entity = require(caregiverId, serviceTypeId);
        caregivers.requireRead(entity.getCaregiver(), isActive(entity), auth);
        return mapper.entityToDto(entity);
    }

    @Transactional
    public CaregiverServiceResponse create(CaregiverServiceCreateRequest dto, Authentication auth) {
        access.requireOwnerOrAdmin(dto.caregiverId(), auth);
        var profile = caregivers.requireAvailable(dto.caregiverId());
        var type = serviceTypes.findById(dto.serviceTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de servicio no encontrado"));
        requireActiveType(type);
        var id = new CaregiverServiceId(dto.caregiverId(), dto.serviceTypeId());
        if (repository.existsById(id)) throw new ConflictException("El cuidador ya ofrece ese servicio");
        var entity = mapper.dtoToEntity(dto);
        entity.setId(id);
        entity.setCaregiver(profile);
        entity.setServiceType(type);
        return mapper.entityToDto(repository.saveAndFlush(entity));
    }

    @Transactional
    public CaregiverServiceResponse update(Long caregiverId, Long serviceTypeId,
                                            CaregiverServiceUpdateRequest dto, Authentication auth) {
        access.requireOwnerOrAdmin(caregiverId, auth);
        var entity = require(caregiverId, serviceTypeId);
        if ("Y".equals(dto.activeFlag())) {
            caregivers.requireAvailable(caregiverId);
            requireActiveType(entity.getServiceType());
        }
        mapper.dtoToEntity(dto, entity);
        return mapper.entityToDto(repository.saveAndFlush(entity));
    }

    @Transactional
    public void delete(Long caregiverId, Long serviceTypeId, Authentication auth) {
        access.requireOwnerOrAdmin(caregiverId, auth);
        repository.delete(require(caregiverId, serviceTypeId));
        repository.flush();
    }

    private CaregiverService require(Long caregiverId, Long serviceTypeId) {
        return repository.findById(new CaregiverServiceId(caregiverId, serviceTypeId))
                .orElseThrow(() -> new ResourceNotFoundException("Servicio del cuidador no encontrado"));
    }

    private boolean isActive(CaregiverService entity) {
        return "Y".equals(entity.getActiveFlag()) && "Y".equals(entity.getServiceType().getActiveFlag());
    }

    private void requireActiveType(ServiceType type) {
        if (!"Y".equals(type.getActiveFlag())) throw new ConflictException("El tipo de servicio está inactivo");
    }
}
