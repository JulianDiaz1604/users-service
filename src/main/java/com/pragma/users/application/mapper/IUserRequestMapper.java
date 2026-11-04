package com.pragma.users.application.mapper;

import com.pragma.users.application.dto.request.CreateEmployeeRequestDto;
import com.pragma.users.application.dto.request.CreateOwnerRequestDto;
import com.pragma.users.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface IUserRequestMapper {
    User toUser(CreateOwnerRequestDto request);

    User toUser(CreateEmployeeRequestDto request);
}
