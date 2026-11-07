package com.icesi.cuidapetproyect.api.service;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.CareApplicationMapper;
import com.icesi.cuidapetproyect.api.security.*;
import com.icesi.cuidapetproyect.exception.*;
import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareApplicationService {
    private final CareApplicationRepository repository;
    private final CareRequestRepository requests;
    private final CaregiverServiceRepository offerings;
    private final CareApplicationMapper mapper;
    private final ResourceAccess access;
    private final CaregiverAccess caregivers;
    private final CareAccess care;

    public List<CareApplicationResponse> findAll(Authentication auth) {
        return repository.findAll().stream()
                .filter(application -> care.canReadApplication(application, auth))
                .map(mapper::entityToDto).toList();
    }

    public CareApplicationResponse findById(Long id, Authentication auth) {
        CareApplication application = require(id);
        care.requireReadApplication(application, auth);
        return mapper.entityToDto(application);
    }

    @Transactional
    public CareApplicationResponse create(CareApplicationCreateRequest dto, Authentication auth) {
        access.requireOwnerOrAdmin(dto.caregiverId(), auth);
        CaregiverProfile caregiver = caregivers.requireAvailable(dto.caregiverId());
        CareRequest request = requests.findById(dto.requestId())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de cuidado no encontrada"));
        if (!"OPEN".equals(request.getRequestStatus())) {
            throw new ConflictException("La solicitud no está abierta a postulaciones");
        }
        if (!request.getStartAt().isAfter(LocalDateTime.now())) {
            throw new ConflictException("La solicitud ya comenzó");
        }
        if (request.getOwner().getId().equals(caregiver.getId())) {
            throw new ConflictException("El propietario no puede postularse a su propia solicitud");
        }
        if (repository.existsByRequest_IdAndCaregiver_Id(request.getId(), caregiver.getId())) {
            throw new ConflictException("El cuidador ya se postuló a esta solicitud");
        }
        requireOffering(caregiver, request);
        requireWithinBudget(dto.proposedRate(), request);
        CareApplication application = mapper.dtoToEntity(dto);
        application.setRequest(request);
        application.setCaregiver(caregiver);
        application.setApplicationStatus("PENDING");
        return mapper.entityToDto(repository.saveAndFlush(application));
    }

    @Transactional
    public CareApplicationResponse update(Long id, CareApplicationUpdateRequest dto, Authentication auth) {
        CareApplication application = require(id);
        care.requireReadApplication(application, auth);
        if (!"PENDING".equals(application.getApplicationStatus())) {
            throw new ConflictException("Solo se pueden modificar postulaciones pendientes");
        }
        Long userId = care.currentUserId(auth);
        boolean admin = access.isAdmin(auth);
        boolean caregiver = application.getCaregiver().getId().equals(userId);
        if ("REJECTED".equals(dto.applicationStatus())) {
            if (!admin && caregiver) {
                throw new AccessDeniedException("Solo el propietario de la solicitud puede rechazar postulaciones");
            }
            application.setApplicationStatus("REJECTED");
            application.setResponseReason(trimToNull(dto.responseReason()));
        } else {
            if (!admin && !caregiver) {
                throw new AccessDeniedException("Solo el cuidador puede modificar o retirar su postulación");
            }
            requireWithinBudget(dto.proposedRate(), application.getRequest());
            mapper.dtoToEntity(dto, application);
            application.setResponseReason(null);
        }
        return mapper.entityToDto(repository.saveAndFlush(application));
    }

    @Transactional
    public void delete(Long id, Authentication auth) {
        CareApplication application = require(id);
        access.requireOwnerOrAdmin(application.getCaregiver().getId(), auth);
        if ("ACCEPTED".equals(application.getApplicationStatus())) {
            throw new ConflictException("No se puede eliminar una postulación aceptada en una reserva");
        }
        repository.delete(application);
        repository.flush();
    }

    private void requireOffering(CaregiverProfile caregiver, CareRequest request) {
        var key = new CaregiverServiceId(caregiver.getId(), request.getServiceType().getId());
        boolean offered = offerings.findById(key).map(offering -> "Y".equals(offering.getActiveFlag())).orElse(false);
        if (!offered) {
            throw new ConflictException("El cuidador no ofrece activamente el servicio solicitado");
        }
    }

    private void requireWithinBudget(BigDecimal rate, CareRequest request) {
        if (request.getMaxBudget() != null && rate.compareTo(request.getMaxBudget()) > 0) {
            throw new ConflictException("La tarifa propuesta supera el presupuesto de la solicitud");
        }
    }

    private CareApplication require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Postulación no encontrada"));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
