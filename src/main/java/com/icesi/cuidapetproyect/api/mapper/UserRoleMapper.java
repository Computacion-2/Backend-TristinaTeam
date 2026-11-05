package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = ApiMapperConfig.class)
public interface UserRoleMapper {

    @Mapping(target = "userId", source = "id.userId")
    @Mapping(target = "roleId", source = "id.roleId")
    @Mapping(target = "assignedById", source = "assignedBy.id")
    UserRoleResponse entityToDto(UserRole entity);

    @Mapping(target = "id.userId", source = "userId")
    @Mapping(target = "user.id", source = "userId")
    @Mapping(target = "id.roleId", source = "roleId")
    @Mapping(target = "role.id", source = "roleId")
    @Mapping(target = "assignedAt", ignore = true)
    @Mapping(target = "assignedBy", ignore = true)
    UserRole dtoToEntity(UserRoleRequest dto);
}
