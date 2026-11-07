package com.icesi.cuidapetproyect.api.service;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.CareUpdatePhotoMapper;
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
public class CareUpdatePhotoService {
    static final int MAX_PHOTOS_PER_UPDATE = 10;

    private final CareUpdatePhotoRepository repository;
    private final CareUpdateRepository updates;
    private final CareUpdatePhotoMapper mapper;
    private final ResourceAccess access;
    private final CareAccess care;

    public List<CareUpdatePhotoResponse> findAll(Authentication auth) {
        return repository.findAll().stream()
                .filter(photo -> care.isBookingParticipant(photo.getCareUpdate().getBooking(), auth))
                .map(mapper::entityToDto).toList();
    }

    public CareUpdatePhotoResponse findById(Long id, Authentication auth) {
        CareUpdatePhoto photo = require(id);
        care.requireBookingParticipant(photo.getCareUpdate().getBooking(), auth);
        return mapper.entityToDto(photo);
    }

    @Transactional
    public CareUpdatePhotoResponse create(CareUpdatePhotoCreateRequest dto, Authentication auth) {
        CareUpdate update = updates.findById(dto.updateId())
                .orElseThrow(() -> new ResourceNotFoundException("Novedad del cuidado no encontrada"));
        access.requireOwnerOrAdmin(update.getCaregiver().getId(), auth);
        requireActiveCare(update);
        String url = dto.photoUrl().trim();
        if (repository.existsByCareUpdate_IdAndPhotoUrl(update.getId(), url)) {
            throw new ConflictException("La foto ya está asociada a esta novedad");
        }
        if (repository.countByCareUpdate_Id(update.getId()) >= MAX_PHOTOS_PER_UPDATE) {
            throw new ConflictException("Una novedad admite como máximo " + MAX_PHOTOS_PER_UPDATE + " fotos");
        }
        CareUpdatePhoto photo = mapper.dtoToEntity(dto);
        photo.setPhotoUrl(url);
        photo.setCareUpdate(update);
        return mapper.entityToDto(repository.saveAndFlush(photo));
    }

    @Transactional
    public CareUpdatePhotoResponse update(Long id, CareUpdatePhotoUpdateRequest dto, Authentication auth) {
        CareUpdatePhoto photo = require(id);
        access.requireOwnerOrAdmin(photo.getCareUpdate().getCaregiver().getId(), auth);
        requireActiveCare(photo.getCareUpdate());
        String url = dto.photoUrl().trim();
        if (repository.existsByCareUpdate_IdAndPhotoUrlAndIdNot(photo.getCareUpdate().getId(), url, id)) {
            throw new ConflictException("La foto ya está asociada a esta novedad");
        }
        mapper.dtoToEntity(dto, photo);
        photo.setPhotoUrl(url);
        return mapper.entityToDto(repository.saveAndFlush(photo));
    }

    @Transactional
    public void delete(Long id, Authentication auth) {
        CareUpdatePhoto photo = require(id);
        access.requireOwnerOrAdmin(photo.getCareUpdate().getCaregiver().getId(), auth);
        photo.getCareUpdate().getPhotos().remove(photo);
        repository.delete(photo);
        repository.flush();
    }

    private void requireActiveCare(CareUpdate update) {
        if (!CareUpdateService.ACTIVE_CARE.contains(update.getBooking().getBookingStatus().getStatusCode())) {
            throw new ConflictException("Las fotos solo se agregan mientras el cuidado está en curso");
        }
    }

    private CareUpdatePhoto require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Foto de la novedad no encontrada"));
    }
}
