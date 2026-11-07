package com.icesi.cuidapetproyect.api.service;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.CareBookingMapper;
import com.icesi.cuidapetproyect.api.security.*;
import com.icesi.cuidapetproyect.exception.*;
import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareBookingService {
    private static final Set<String> FINISHED = Set.of("COMPLETED", "CANCELLED");

    private final CareBookingRepository repository;
    private final CareApplicationRepository applications;
    private final BookingStatusRepository statuses;
    private final CareBookingMapper mapper;
    private final ResourceAccess access;
    private final CaregiverAccess caregivers;
    private final CareAccess care;
    private final CareNotifier notifier;

    public List<CareBookingResponse> findAll(Authentication auth) {
        return repository.findAll().stream()
                .filter(booking -> care.isBookingParticipant(booking, auth))
                .map(mapper::entityToDto).toList();
    }

    public CareBookingResponse findById(Long id, Authentication auth) {
        CareBooking booking = require(id);
        care.requireBookingParticipant(booking, auth);
        return mapper.entityToDto(booking);
    }

    @Transactional
    public CareBookingResponse create(CareBookingCreateRequest dto, Authentication auth) {
        CareApplication application = applications.findById(dto.applicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Postulación no encontrada"));
        CareRequest request = application.getRequest();
        access.requireOwnerOrAdmin(request.getOwner().getId(), auth);
        if (!"PENDING".equals(application.getApplicationStatus())) {
            throw new ConflictException("Solo se puede aceptar una postulación pendiente");
        }
        if (!"OPEN".equals(request.getRequestStatus()) || repository.findByRequest_Id(request.getId()).isPresent()) {
            throw new ConflictException("La solicitud ya no está abierta para reservar");
        }
        if (!request.getStartAt().isAfter(LocalDateTime.now())) {
            throw new ConflictException("La solicitud ya comenzó");
        }
        caregivers.requireAvailable(application.getCaregiver().getId());
        CareBooking booking = mapper.dtoToEntity(dto);
        booking.setExactAddress(dto.exactAddress().trim());
        booking.setRequest(request);
        booking.setAcceptedApplication(application);
        booking.setOwner(request.getOwner());
        booking.setCaregiver(application.getCaregiver());
        booking.setAgreedRate(application.getProposedRate());
        booking.setBookingStatus(status("SCHEDULED"));
        booking = repository.saveAndFlush(booking);
        application.setApplicationStatus("ACCEPTED");
        application.setBooking(booking);
        for (CareApplication other : applications.findByRequest_Id(request.getId())) {
            if (!other.getId().equals(application.getId()) && "PENDING".equals(other.getApplicationStatus())) {
                other.setApplicationStatus("REJECTED");
                other.setResponseReason("Se aceptó otra postulación");
            }
        }
        request.setRequestStatus("ASSIGNED");
        request.setBooking(booking);
        notifier.notify(booking.getCaregiver().getUser(), "BOOKING_CREATED", "Nueva reserva",
                "Tu postulación fue aceptada para la solicitud " + request.getId(), "CARE_BOOKING", booking.getId());
        return mapper.entityToDto(booking);
    }

    @Transactional
    public CareBookingResponse update(Long id, CareBookingUpdateRequest dto, Authentication auth) {
        CareBooking booking = require(id);
        care.requireBookingParticipant(booking, auth);
        String current = booking.getBookingStatus().getStatusCode();
        String target = dto.bookingStatusCode();
        if (FINISHED.contains(current)) {
            throw new ConflictException("La reserva ya finalizó");
        }
        boolean admin = access.isAdmin(auth);
        Long userId = care.currentUserId(auth);
        boolean owner = admin || booking.getOwner().getId().equals(userId);
        boolean caregiver = admin || booking.getCaregiver().getId().equals(userId);
        String address = dto.exactAddress().trim();
        if (!address.equals(booking.getExactAddress())) {
            if (!owner) throw new AccessDeniedException("Solo el propietario cambia la dirección");
            if (!"SCHEDULED".equals(current)) {
                throw new ConflictException("La dirección solo se cambia antes de iniciar el cuidado");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        if (!target.equals(current)) {
            switch (target) {
                case "IN_PROGRESS" -> {
                    requireTransition("SCHEDULED".equals(current));
                    if (!caregiver) throw new AccessDeniedException("Solo el cuidador inicia el cuidado");
                    booking.setStartedAt(now);
                }
                case "PENDING_CONFIRMATION" -> {
                    requireTransition("IN_PROGRESS".equals(current));
                    if (!caregiver) throw new AccessDeniedException("Solo el cuidador marca el cuidado como terminado");
                    booking.setCaregiverDoneFlag("Y");
                }
                case "COMPLETED" -> {
                    requireTransition("PENDING_CONFIRMATION".equals(current));
                    if (!owner) throw new AccessDeniedException("Solo el propietario confirma la finalización");
                    booking.setOwnerConfirmedFlag("Y");
                    booking.setCompletedAt(now);
                    booking.getRequest().setRequestStatus("CLOSED");
                }
                case "CANCELLED" -> {
                    if (dto.cancellationReason() == null || dto.cancellationReason().isBlank()) {
                        throw new IllegalArgumentException("Se requiere el motivo de cancelación");
                    }
                    String reason = dto.cancellationReason().trim();
                    booking.setCancellationReason(reason);
                    booking.setCancelledAt(now);
                    booking.getRequest().setRequestStatus("CANCELLED");
                    booking.getRequest().setCancellationReason(reason);
                }
                default -> requireTransition(false);
            }
            booking.setBookingStatus(status(target));
        }
        mapper.dtoToEntity(dto, booking);
        booking.setExactAddress(address);
        booking = repository.saveAndFlush(booking);
        if (!target.equals(current)) {
            AppUser recipient = booking.getOwner().getId().equals(userId)
                    ? booking.getCaregiver().getUser() : booking.getOwner();
            notifier.notify(recipient, "BOOKING_STATUS", "Reserva actualizada",
                    "La reserva " + booking.getId() + " cambió a " + target, "CARE_BOOKING", booking.getId());
        }
        return mapper.entityToDto(booking);
    }

    @Transactional
    public void delete(Long id, Authentication auth) {
        access.requireAdmin(auth);
        CareBooking booking = require(id);
        CareRequest request = booking.getRequest();
        CareApplication application = booking.getAcceptedApplication();
        if ("ASSIGNED".equals(request.getRequestStatus())) {
            request.setRequestStatus("OPEN");
            application.setApplicationStatus("PENDING");
        }
        // Relaciones inversas: se limpian para que Hibernate no conserve la reserva borrada.
        request.setBooking(null);
        application.setBooking(null);
        repository.delete(booking);
        repository.flush();
    }

    private void requireTransition(boolean allowed) {
        if (!allowed) throw new ConflictException("Transición de estado de la reserva no permitida");
    }

    private BookingStatus status(String code) {
        return statuses.findById(code)
                .orElseThrow(() -> new ConflictException("El catálogo no tiene el estado " + code));
    }

    private CareBooking require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
    }
}
