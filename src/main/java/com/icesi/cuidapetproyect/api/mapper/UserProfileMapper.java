package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.UserProfile;
import org.mapstruct.*;

@Mapper(config = ApiMapperConfig.class)
public interface UserProfileMapper {
    @Mapping(target = "userId", source = "id")
    UserProfileResponse entityToDto(UserProfile entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "profileUpdatedAt", ignore = true)
    UserProfile dtoToEntity(UserProfileCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "profileUpdatedAt", ignore = true)
    void dtoToEntity(UserProfileUpdateRequest dto, @MappingTarget UserProfile entity);
}
