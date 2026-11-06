package com.icesi.cuidapetproyect.api.service;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.PetPhotoMapper;
import com.icesi.cuidapetproyect.api.security.ResourceAccess;
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
public class PetPhotoService {
    private final PetPhotoRepository photos;
    private final PetRepository pets;
    private final PetPhotoMapper mapper;
    private final ResourceAccess access;

    public List<PetPhotoResponse> findAll(Authentication authentication) {
        List<PetPhoto> visible = access.isAdmin(authentication) ? photos.findAll()
                : photos.findByPet_Owner_Id(access.currentUser(authentication).getId());
        return visible.stream().map(mapper::entityToDto).toList();
    }

    public PetPhotoResponse findById(Long id, Authentication authentication) {
        return mapper.entityToDto(requireOwnedPhoto(id, authentication));
    }

    @Transactional
    public PetPhotoResponse create(PetPhotoCreateRequest dto, Authentication authentication) {
        Pet pet = lockOwnedPet(dto.petId(), authentication);
        if (photos.existsByPet_IdAndPhotoUrl(dto.petId(), dto.photoUrl())) {
            throw new ConflictException("La mascota ya tiene esa foto");
        }
        PetPhoto photo = mapper.dtoToEntity(dto);
        photo.setPet(pet);
        if ("Y".equals(dto.mainPhotoFlag())) clearMainPhoto(pet.getId(), null);
        return mapper.entityToDto(photos.saveAndFlush(photo));
    }

    @Transactional
    public PetPhotoResponse update(Long id, PetPhotoUpdateRequest dto, Authentication authentication) {
        PetPhoto photo = requireOwnedPhoto(id, authentication);
        lockOwnedPet(photo.getPet().getId(), authentication);
        if (photos.existsByPet_IdAndPhotoUrlAndIdNot(photo.getPet().getId(), dto.photoUrl(), id)) {
            throw new ConflictException("La mascota ya tiene esa foto");
        }
        if ("Y".equals(dto.mainPhotoFlag())) clearMainPhoto(photo.getPet().getId(), id);
        mapper.dtoToEntity(dto, photo);
        return mapper.entityToDto(photos.saveAndFlush(photo));
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        PetPhoto photo = requireOwnedPhoto(id, authentication);
        lockOwnedPet(photo.getPet().getId(), authentication);
        photos.delete(photo);
        photos.flush();
    }

    private PetPhoto requireOwnedPhoto(Long id, Authentication authentication) {
        PetPhoto photo = photos.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Foto no encontrada"));
        access.requireOwnerOrAdmin(photo.getPet().getOwner().getId(), authentication);
        return photo;
    }

    private Pet lockOwnedPet(Long id, Authentication authentication) {
        Pet pet = pets.findForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada"));
        access.requireOwnerOrAdmin(pet.getOwner().getId(), authentication);
        return pet;
    }

    private void clearMainPhoto(Long petId, Long exceptId) {
        // El bloqueo de la mascota serializa la elección de su foto principal.
        for (PetPhoto current : photos.findByPet_Id(petId)) {
            if (!current.getId().equals(exceptId) && "Y".equals(current.getMainPhotoFlag())) {
                current.setMainPhotoFlag("N");
            }
        }
        photos.flush();
    }
}
