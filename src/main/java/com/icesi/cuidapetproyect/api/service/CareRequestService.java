package com.icesi.cuidapetproyect.api.service;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.CareRequestMapper;
import com.icesi.cuidapetproyect.api.security.*;
import com.icesi.cuidapetproyect.exception.*;
import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareRequestService {
    private final CareRequestRepository repository;
    private final CareApplicationRepository applications;
    private final CareBookingRepository bookings;
    private final AppUserRepository users;
    private final PetRepository pets;
    private final ServiceTypeRepository serviceTypes;
    private final CareRequestMapper mapper;
    private final ResourceAccess access;
    private final CareAccess care;

    public List<CareRequestResponse> findAll(Authentication auth) {
        return repository.findAll().stream()
                .filter(request -> care.canReadRequest(request, auth))
                .map(mapper::entityToDto).toList();
    }

    public CareRequestResponse findById(Long id, Authentication auth) {
        CareRequest request = require(id);
        care.requireReadRequest(request, auth);
        return mapper.entityToDto(request);
    }

    @Transactional
    public CareRequestResponse create(CareRequestCreateRequest dto, Authentication auth) {
        access.requireOwnerOrAdmin(dto.ownerId(), auth);
        AppUser owner = users.findById(dto.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException("Propietario no encontrado"));
        if (!"ACTIVE".equals(owner.getAccountStatus())) {
            throw new ConflictException("El propietario debe estar activo");
        }
        validatePeriod(dto.startAt(), dto.endAt());
        CareRequest request = mapper.dtoToEntity(dto);
        request.setOwner(owner);
        request.setPet(requireOwnedActivePet(dto.petId(), owner.getId()));
        request.setServiceType(requireActiveServiceType(dto.serviceTypeId()));
        request.setRequestStatus("OPEN");
        return mapper.entityToDto(repository.saveAndFlush(request));
    }

    @Transactional
    public CareRequestResponse update(Long id, CareRequestUpdateRequest dto, Authentication auth) {
        CareRequest request = require(id);
        access.requireOwnerOrAdmin(request.getOwner().getId(), auth);
        if (!"OPEN".equals(request.getRequestStatus())) {
            throw new ConflictException("Solo se pueden modificar solicitudes abiertas");
        }
        validatePeriod(dto.startAt(), dto.endAt());
        boolean datesChanged = !dto.startAt().equals(request.getStartAt()) || !dto.endAt().equals(request.getEndAt());
        if (datesChanged && !dto.startAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("La nueva fecha de inicio debe ser futura");
        }
        boolean closing = !"OPEN".equals(dto.requestStatus());
        if ("CANCELLED".equals(dto.requestStatus()) && isBlank(dto.cancellationReason())) {
            throw new IllegalArgumentException("Se requiere el motivo de cancelación");
        }
        if (!request.getPet().getId().equals(dto.petId())) {
            request.setPet(requireOwnedActivePet(dto.petId(), request.getOwner().getId()));
        }
        if (!request.getServiceType().getId().equals(dto.serviceTypeId())) {
            request.setServiceType(requireActiveServiceType(dto.serviceTypeId()));
        }
        mapper.dtoToEntity(dto, request);
        if (!"CANCELLED".equals(dto.requestStatus())) {
            request.setCancellationReason(null);
        }
        if (closing) {
            rejectPendingApplications(request, "CANCELLED".equals(dto.requestStatus())
                    ? "La solicitud fue cancelada" : "La solicitud fue cerrada");
        }
        return mapper.entityToDto(repository.saveAndFlush(request));
    }

    @Transactional
    public void delete(Long id, Authentication auth) {
        CareRequest request = require(id);
        access.requireOwnerOrAdmin(request.getOwner().getId(), auth);
        if (bookings.findByRequest_Id(id).isPresent()) {
            throw new ConflictException("No se puede eliminar una solicitud con reserva");
        }
        applications.deleteAll(applications.findByRequest_Id(id));
        applications.flush();
        repository.delete(request);
        repository.flush();
    }

    private void rejectPendingApplications(CareRequest request, String reason) {
        for (CareApplication application : applications.findByRequest_Id(request.getId())) {
            if ("PENDING".equals(application.getApplicationStatus())) {
                application.setApplicationStatus("REJECTED");
                application.setResponseReason(reason);
            }
        }
    }

    private CareRequest require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de cuidado no encontrada"));
    }

    private Pet requireOwnedActivePet(Long petId, Long ownerId) {
        Pet pet = pets.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada"));
        if (!pet.getOwner().getId().equals(ownerId)) {
            throw new ConflictException("La mascota no pertenece al propietario de la solicitud");
        }
        if (!"ACTIVE".equals(pet.getPetStatus())) {
            throw new ConflictException("La mascota debe estar activa");
        }
        return pet;
    }

    private ServiceType requireActiveServiceType(Long id) {
        ServiceType type = serviceTypes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de servicio no encontrado"));
        if (!"Y".equals(type.getActiveFlag())) {
            throw new ConflictException("El tipo de servicio debe estar activo");
        }
        return type;
    }

    private void validatePeriod(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("La fecha final debe ser posterior a la inicial");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
