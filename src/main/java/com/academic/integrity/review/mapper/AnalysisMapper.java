package com.academic.integrity.review.mapper;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.dto.AnalysisResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AnalysisMapper {

	@Mapping(target = "documentId", source = "document.id")
	AnalysisResponseDTO toDto(Analysis analysis);
}
