package com.icesi.cuidapetproyect.api.mapper;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.model.UserReview;
import org.mapstruct.*;

@Mapper(config = ApiMapperConfig.class)
public interface UserReviewMapper {
    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "reviewerId", source = "reviewer.id")
    @Mapping(target = "reviewedUserId", source = "reviewedUser.id")
    UserReviewResponse entityToDto(UserReview entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "reviewer", ignore = true)
    @Mapping(target = "reviewedUser", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    UserReview dtoToEntity(UserReviewCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "reviewer", ignore = true)
    @Mapping(target = "reviewedUser", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void dtoToEntity(UserReviewUpdateRequest dto, @MappingTarget UserReview entity);
}
