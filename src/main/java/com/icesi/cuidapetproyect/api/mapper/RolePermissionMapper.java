package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.RolePermission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = ApiMapperConfig.class)
public interface RolePermissionMapper {

    @Mapping(target = "roleId", source = "id.roleId")
    @Mapping(target = "permissionId", source = "id.permissionId")
    RolePermissionResponse entityToDto(RolePermission entity);

    @Mapping(target = "id.roleId", source = "roleId")
    @Mapping(target = "role.id", source = "roleId")
    @Mapping(target = "id.permissionId", source = "permissionId")
    @Mapping(target = "permission.id", source = "permissionId")
    @Mapping(target = "grantedAt", ignore = true)
    RolePermission dtoToEntity(RolePermissionRequest dto);
}
