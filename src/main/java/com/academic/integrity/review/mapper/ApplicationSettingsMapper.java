package com.academic.integrity.review.mapper;

import com.academic.integrity.review.domain.ApplicationSettings;
import com.academic.integrity.review.dto.ApplicationSettingsResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApplicationSettingsMapper {
	ApplicationSettingsResponseDTO toDto(ApplicationSettings settings);
}
