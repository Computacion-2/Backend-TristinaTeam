package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.AppUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = ApiMapperConfig.class)
public interface AppUserMapper {

    UserResponse entityToDto(AppUser entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "accountStatus", ignore = true)
    @Mapping(target = "passwordHash", source = "password")
    AppUser dtoToEntity(UserCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "passwordHash", source = "password")
    AppUser dtoToEntity(UserUpdateRequest dto);
}
