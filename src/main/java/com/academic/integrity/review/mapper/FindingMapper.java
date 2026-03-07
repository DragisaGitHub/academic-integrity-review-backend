package com.academic.integrity.review.mapper;

import com.academic.integrity.review.domain.Finding;
import com.academic.integrity.review.dto.FindingResponseDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FindingMapper {

	@Mapping(target = "analysisId", source = "analysis.id")
	FindingResponseDTO toDto(Finding finding);

	List<FindingResponseDTO> toDtoList(List<Finding> findings);
}
