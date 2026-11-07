package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.FavoriteCaregiver;
import org.mapstruct.*;

@Mapper(config = ApiMapperConfig.class)
public interface FavoriteCaregiverMapper {
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "caregiverId", source = "caregiver.id")
    FavoriteCaregiverResponse entityToDto(FavoriteCaregiver entity);

    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "caregiver", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    FavoriteCaregiver dtoToEntity(FavoriteCaregiverCreateRequest dto);
}
