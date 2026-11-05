package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.AppPermission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = ApiMapperConfig.class)
public interface AppPermissionMapper {

    PermissionResponse entityToDto(AppPermission entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "rolePermissions", ignore = true)
    AppPermission dtoToEntity(PermissionCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "rolePermissions", ignore = true)
    @Mapping(target = "permissionCode", ignore = true)
    AppPermission dtoToEntity(PermissionUpdateRequest dto);
}
