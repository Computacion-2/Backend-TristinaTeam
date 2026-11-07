package com.icesi.cuidapetproyect.api.service;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.mapper.AppNotificationMapper;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppNotificationService {
    private final AppNotificationRepository repository;
    private final AppUserRepository users;
    private final AppNotificationMapper mapper;
    private final ResourceAccess access;

    public List<NotificationResponse> findAll(Authentication auth) {
        List<AppNotification> visible = access.isAdmin(auth) ? repository.findAll()
                : repository.findByUser_Id(access.currentUser(auth).getId());
        return visible.stream()
                .sorted(Comparator.comparing(AppNotification::getCreatedAt).reversed())
                .map(mapper::entityToDto).toList();
    }

    public NotificationResponse findById(Long id, Authentication auth) {
        return mapper.entityToDto(requireOwned(id, auth));
    }

    @Transactional
    public NotificationResponse create(NotificationCreateRequest dto, Authentication auth) {
        access.requireAdmin(auth);
        AppUser recipient = users.findById(dto.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Destinatario no encontrado"));
        if (!"ACTIVE".equals(recipient.getAccountStatus())) {
            throw new ConflictException("El destinatario debe tener una cuenta activa");
        }
        if ((dto.referenceEntity() == null) != (dto.referenceId() == null)) {
            throw new IllegalArgumentException("referenceEntity y referenceId se envían juntos");
        }
        AppNotification notification = mapper.dtoToEntity(dto);
        notification.setUser(recipient);
        notification.setTitle(dto.title().trim());
        notification.setMessageText(dto.messageText().trim());
        notification.setReadFlag("N");
        return mapper.entityToDto(repository.saveAndFlush(notification));
    }

    @Transactional
    public NotificationResponse update(Long id, NotificationUpdateRequest dto, Authentication auth) {
        AppNotification notification = requireOwned(id, auth);
        boolean changesContent = (dto.title() != null && !Objects.equals(dto.title(), notification.getTitle()))
                || (dto.messageText() != null && !Objects.equals(dto.messageText(), notification.getMessageText()));
        if (changesContent && !access.isAdmin(auth)) {
            throw new AccessDeniedException("El destinatario solo marca la notificación como leída o no leída");
        }
        boolean wasRead = "Y".equals(notification.getReadFlag());
        mapper.dtoToEntity(dto, notification);
        if ("Y".equals(dto.readFlag()) && !wasRead) notification.setReadAt(LocalDateTime.now());
        if ("N".equals(dto.readFlag())) notification.setReadAt(null);
        return mapper.entityToDto(repository.saveAndFlush(notification));
    }

    @Transactional
    public void delete(Long id, Authentication auth) {
        repository.delete(requireOwned(id, auth));
        repository.flush();
    }

    private AppNotification requireOwned(Long id, Authentication auth) {
        AppNotification notification = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));
        access.requireOwnerOrAdmin(notification.getUser().getId(), auth);
        return notification;
    }
}
