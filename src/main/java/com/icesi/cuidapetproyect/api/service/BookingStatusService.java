package com.icesi.cuidapetproyect.api.service;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.BookingStatusMapper;
import com.icesi.cuidapetproyect.api.security.ResourceAccess;
import com.icesi.cuidapetproyect.exception.*;
import com.icesi.cuidapetproyect.model.BookingStatus;
import com.icesi.cuidapetproyect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingStatusService {
    private final BookingStatusRepository repository;
    private final CareBookingRepository bookings;
    private final BookingStatusMapper mapper;
    private final ResourceAccess access;

    public List<BookingStatusResponse> findAll() {
        return repository.findAll(Sort.by("statusCode")).stream().map(mapper::entityToDto).toList();
    }

    public BookingStatusResponse findById(String code) {
        return mapper.entityToDto(require(code));
    }

    @Transactional
    public BookingStatusResponse create(BookingStatusCreateRequest dto, Authentication auth) {
        access.requireAdmin(auth);
        String name = dto.statusName().trim();
        if (repository.existsById(dto.statusCode())) {
            throw new ConflictException("El estado de reserva ya existe");
        }
        if (repository.existsByStatusNameIgnoreCase(name)) {
            throw new ConflictException("Ya existe un estado de reserva con ese nombre");
        }
        BookingStatus entity = mapper.dtoToEntity(dto);
        entity.setStatusName(name);
        return mapper.entityToDto(repository.saveAndFlush(entity));
    }

    @Transactional
    public BookingStatusResponse update(String code, BookingStatusUpdateRequest dto, Authentication auth) {
        access.requireAdmin(auth);
        BookingStatus entity = require(code);
        String name = dto.statusName().trim();
        if (repository.existsByStatusNameIgnoreCaseAndStatusCodeNot(name, code)) {
            throw new ConflictException("Ya existe un estado de reserva con ese nombre");
        }
        mapper.dtoToEntity(dto, entity);
        entity.setStatusName(name);
        return mapper.entityToDto(repository.saveAndFlush(entity));
    }

    @Transactional
    public void delete(String code, Authentication auth) {
        access.requireAdmin(auth);
        BookingStatus entity = require(code);
        if (bookings.existsByBookingStatus_StatusCode(code)) {
            throw new ConflictException("El estado está asignado a reservas existentes");
        }
        repository.delete(entity);
        repository.flush();
    }

    private BookingStatus require(String code) {
        return repository.findById(code)
                .orElseThrow(() -> new ResourceNotFoundException("Estado de reserva no encontrado"));
    }
}
