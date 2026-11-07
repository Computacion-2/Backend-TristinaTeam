package com.icesi.cuidapetproyect.api.service;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.UserReviewMapper;
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
public class UserReviewService {
    private final UserReviewRepository repository;
    private final CareBookingRepository bookings;
    private final AppUserRepository users;
    private final UserReviewMapper mapper;
    private final ResourceAccess access;
    private final CareNotifier notifier;

    public List<UserReviewResponse> findAll(Authentication auth) {
        return repository.findAll().stream().map(mapper::entityToDto).toList();
    }

    public UserReviewResponse findById(Long id, Authentication auth) {
        return mapper.entityToDto(require(id));
    }

    @Transactional
    public UserReviewResponse create(UserReviewCreateRequest dto, Authentication auth) {
        access.requireOwnerOrAdmin(dto.reviewerId(), auth);
        CareBooking booking = bookings.findById(dto.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
        if (!"COMPLETED".equals(booking.getBookingStatus().getStatusCode())) {
            throw new ConflictException("Solo se reseñan reservas completadas");
        }
        AppUser reviewer = users.findById(dto.reviewerId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        AppUser reviewed;
        if (booking.getOwner().getId().equals(reviewer.getId())) {
            reviewed = booking.getCaregiver().getUser();
        } else if (booking.getCaregiver().getId().equals(reviewer.getId())) {
            reviewed = booking.getOwner();
        } else {
            throw new ConflictException("Solo los participantes de la reserva pueden reseñarla");
        }
        if (repository.existsByBooking_IdAndReviewer_Id(booking.getId(), reviewer.getId())) {
            throw new ConflictException("El usuario ya reseñó esta reserva");
        }
        UserReview review = mapper.dtoToEntity(dto);
        review.setBooking(booking);
        review.setReviewer(reviewer);
        review.setReviewedUser(reviewed);
        review.setReviewComment(trimToNull(dto.reviewComment()));
        review = repository.saveAndFlush(review);
        notifier.notify(reviewed, "REVIEW_RECEIVED", "Nueva reseña",
                "Recibiste una calificación de " + review.getRating() + " estrellas", "USER_REVIEW", review.getId());
        return mapper.entityToDto(review);
    }

    @Transactional
    public UserReviewResponse update(Long id, UserReviewUpdateRequest dto, Authentication auth) {
        UserReview review = require(id);
        access.requireOwnerOrAdmin(review.getReviewer().getId(), auth);
        mapper.dtoToEntity(dto, review);
        review.setReviewComment(trimToNull(dto.reviewComment()));
        return mapper.entityToDto(repository.saveAndFlush(review));
    }

    @Transactional
    public void delete(Long id, Authentication auth) {
        UserReview review = require(id);
        access.requireOwnerOrAdmin(review.getReviewer().getId(), auth);
        repository.delete(review);
        repository.flush();
    }

    private UserReview require(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reseña no encontrada"));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
