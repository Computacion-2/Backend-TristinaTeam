package com.icesi.cuidapetproyect.api.service;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.CareUpdateMapper;
import com.icesi.cuidapetproyect.api.security.*;
import com.icesi.cuidapetproyect.exception.*;
import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareUpdateService {
    static final Set<String> ACTIVE_CARE = Set.of("IN_PROGRESS", "PENDING_CONFIRMATION");

    private final CareUpdateRepository repository;
    private final CareUpdatePhotoRepository photos;
    private final CareBookingRepository bookings;
    private final CareUpdateMapper mapper;
    private final ResourceAccess access;
    private final CareAccess care;
    private final CareNotifier notifier;

    public List<CareUpdateResponse> findAll(Authentication auth) {
        return repository.findAll().stream()
                .filter(update -> care.isBookingParticipant(update.getBooking(), auth))
                .map(mapper::entityToDto).toList();
    }

    public CareUpdateResponse findById(Long id, Authentication auth) {
        CareUpdate update = require(id);
        care.requireBookingParticipant(update.getBooking(), auth);
        return mapper.entityToDto(update);
    }

    @Transactional
    public CareUpdateResponse create(CareUpdateCreateRequest dto, Authentication auth) {
        CareBooking booking = bookings.findById(dto.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
        access.requireOwnerOrAdmin(booking.getCaregiver().getId(), auth);
        requireActiveCare(booking);
        CareUpdate update = mapper.dtoToEntity(dto);
        update.setUpdateMessage(dto.updateMessage().trim());
        update.setBooking(booking);
        update.setCaregiver(booking.getCaregiver());
        update = repository.saveAndFlush(update);
        notifier.notify(booking.getOwner(), "CARE_UPDATE", "Nueva novedad del cuidado",
                "El cuidador registró una novedad en la reserva " + booking.getId(), "CARE_UPDATE", update.getId());
        return mapper.entityToDto(update);
    }

    @Transactional
    public CareUpdateResponse update(Long id, CareUpdateUpdateRequest dto, Authentication auth) {
        CareUpdate update = require(id);
        access.requireOwnerOrAdmin(update.getCaregiver().getId(), auth);
        requireActiveCare(update.getBooking());
        mapper.dtoToEntity(dto, update);
        update.setUpdateMessage(dto.updateMessage().trim());
        return mapper.entityToDto(repository.saveAndFlush(update));
    }

    @Transactional
    public void delete(Long id, Authentication auth) {
        CareUpdate update = require(id);
        access.requireOwnerOrAdmin(update.getCaregiver().getId(), auth);
        photos.deleteAll(photos.findByCareUpdate_Id(id));
        photos.flush();
        update.getPhotos().clear();
        repository.delete(update);
        repository.flush();
    }

    private void requireActiveCare(CareBooking booking) {
        if (!ACTIVE_CARE.contains(booking.getBookingStatus().getStatusCode())) {
            throw new ConflictException("Las novedades solo se registran mientras el cuidado está en curso");
        }
    }

    private CareUpdate require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Novedad del cuidado no encontrada"));
    }
}
