package com.academic.integrity.review.mapper;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.dto.AnalysisResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = DocumentMapper.class)
public interface AnalysisMapper {

	AnalysisResponseDTO toDto(Analysis analysis);
}
