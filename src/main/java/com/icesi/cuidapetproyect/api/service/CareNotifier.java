package com.icesi.cuidapetproyect.api.service;

import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.AppNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Registra notificaciones automáticas del flujo de cuidado para usuarios activos. */
@Component
@RequiredArgsConstructor
public class CareNotifier {
    private final AppNotificationRepository notifications;

    public void notify(AppUser recipient, String type, String title, String message, String entity, Long referenceId) {
        if (recipient == null || !"ACTIVE".equals(recipient.getAccountStatus())) return;
        var notification = new AppNotification();
        notification.setUser(recipient);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setMessageText(message);
        notification.setReferenceEntity(entity);
        notification.setReferenceId(referenceId);
        notifications.save(notification);
    }
}
