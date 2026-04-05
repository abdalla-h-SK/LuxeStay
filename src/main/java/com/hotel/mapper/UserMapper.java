package com.hotel.mapper;

import com.hotel.dto.response.UserResponse;
import com.hotel.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "verified", source = "verified")
    UserResponse toResponse(User user);
}
