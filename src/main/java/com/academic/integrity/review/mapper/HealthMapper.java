package com.academic.integrity.review.mapper;

import com.academic.integrity.review.dto.HealthResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HealthMapper {

	@Mapping(target = "status", source = "status")
	HealthResponseDTO toDto(String status);
}
