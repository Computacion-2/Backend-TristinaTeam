package com.icesi.cuidapetproyect.api.service;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.IncidentReportMapper;
import com.icesi.cuidapetproyect.api.security.ResourceAccess;
import com.icesi.cuidapetproyect.exception.*;
import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentReportService {
    private static final Set<String> CLOSED = Set.of("RESOLVED", "DISMISSED");

    private final IncidentReportRepository repository;
    private final AppUserRepository users;
    private final CareRequestRepository requests;
    private final CareBookingRepository bookings;
    private final CareApplicationRepository applications;
    private final IncidentReportMapper mapper;
    private final ResourceAccess access;
    private final CareNotifier notifier;

    public List<IncidentReportResponse> findAll(Authentication auth) {
        List<IncidentReport> visible = access.isAdmin(auth) ? repository.findAll()
                : repository.findByReporter_Id(access.currentUser(auth).getId());
        return visible.stream().map(mapper::entityToDto).toList();
    }

    public IncidentReportResponse findById(Long id, Authentication auth) {
        IncidentReport report = require(id);
        access.requireOwnerOrAdmin(report.getReporter().getId(), auth);
        return mapper.entityToDto(report);
    }

    @Transactional
    public IncidentReportResponse create(IncidentReportCreateRequest dto, Authentication auth) {
        access.requireOwnerOrAdmin(dto.reporterId(), auth);
        if (dto.reporterId().equals(dto.reportedUserId())) {
            throw new ConflictException("No se puede reportar a uno mismo");
        }
        AppUser reporter = user(dto.reporterId());
        AppUser reported = user(dto.reportedUserId());
        CareRequest request = requests.findById(dto.requestId())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de cuidado no encontrada"));
        CareBooking booking = null;
        if (dto.bookingId() != null) {
            booking = bookings.findById(dto.bookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
            if (!booking.getRequest().getId().equals(request.getId())) {
                throw new ConflictException("La reserva no corresponde a la solicitud indicada");
            }
        }
        Set<Long> participants = participants(request, booking);
        if (!participants.contains(reporter.getId()) || !participants.contains(reported.getId())) {
            throw new ConflictException("Quien reporta y la persona reportada deben participar en el cuidado");
        }
        IncidentReport report = mapper.dtoToEntity(dto);
        report.setReporter(reporter);
        report.setReportedUser(reported);
        report.setRequest(request);
        report.setBooking(booking);
        report.setReportStatus("OPEN");
        return mapper.entityToDto(repository.saveAndFlush(report));
    }

    @Transactional
    public IncidentReportResponse update(Long id, IncidentReportUpdateRequest dto, Authentication auth) {
        IncidentReport report = require(id);
        access.requireOwnerOrAdmin(report.getReporter().getId(), auth);
        boolean admin = access.isAdmin(auth);
        String previous = report.getReportStatus();
        if (!admin) {
            if (!"OPEN".equals(previous)) {
                throw new ConflictException("Solo se edita un reporte abierto");
            }
            if (!"OPEN".equals(dto.reportStatus()) || dto.resolutionNotes() != null) {
                throw new AccessDeniedException("Solo ADMIN revisa y resuelve reportes");
            }
        }
        boolean closing = CLOSED.contains(dto.reportStatus());
        if (closing && (dto.resolutionNotes() == null || dto.resolutionNotes().isBlank())) {
            throw new IllegalArgumentException("Se requieren notas de resolución");
        }
        mapper.dtoToEntity(dto, report);
        if (closing) {
            if (!CLOSED.contains(previous)) {
                report.setResolvedBy(access.currentUser(auth));
                report.setResolvedAt(LocalDateTime.now());
            }
        } else {
            report.setResolvedBy(null);
            report.setResolvedAt(null);
        }
        report = repository.saveAndFlush(report);
        if (!dto.reportStatus().equals(previous)) {
            notifier.notify(report.getReporter(), "INCIDENT_STATUS", "Reporte actualizado",
                    "Tu reporte " + report.getId() + " cambió a " + dto.reportStatus(), "INCIDENT_REPORT", report.getId());
        }
        return mapper.entityToDto(report);
    }

    @Transactional
    public void delete(Long id, Authentication auth) {
        IncidentReport report = require(id);
        access.requireOwnerOrAdmin(report.getReporter().getId(), auth);
        if (!access.isAdmin(auth) && !"OPEN".equals(report.getReportStatus())) {
            throw new ConflictException("Solo se elimina un reporte que sigue abierto");
        }
        repository.delete(report);
        repository.flush();
    }

    private Set<Long> participants(CareRequest request, CareBooking booking) {
        Set<Long> ids = new HashSet<>();
        if (booking != null) {
            ids.add(booking.getOwner().getId());
            ids.add(booking.getCaregiver().getId());
            return ids;
        }
        ids.add(request.getOwner().getId());
        applications.findByRequest_Id(request.getId()).forEach(a -> ids.add(a.getCaregiver().getId()));
        return ids;
    }

    private AppUser user(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private IncidentReport require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte de incidente no encontrado"));
    }
}
