package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.AppNotification;
import org.mapstruct.*;

@Mapper(config = ApiMapperConfig.class)
public interface AppNotificationMapper {
    @Mapping(target = "userId", source = "user.id")
    NotificationResponse entityToDto(AppNotification entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "readFlag", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    AppNotification dtoToEntity(NotificationCreateRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "notificationType", ignore = true)
    @Mapping(target = "referenceEntity", ignore = true)
    @Mapping(target = "referenceId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    void dtoToEntity(NotificationUpdateRequest dto, @MappingTarget AppNotification entity);
}
