
package com.icesi.cuidapetproyect.repository;

import com.icesi.cuidapetproyect.model.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppNotificationRepository
        extends JpaRepository<AppNotification, Long> {

    List<AppNotification> findByUser_Id(Long userId);

    List<AppNotification> findByUser_IdAndReadFlag(
            Long userId, String readFlag);
}
