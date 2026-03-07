package com.academic.integrity.review.mapper;

import com.academic.integrity.review.domain.Document;
import com.academic.integrity.review.dto.DocumentResponseDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentMapper {
	@Mapping(target = "hasAnalysis", ignore = true)
	@Mapping(target = "analysisId", ignore = true)
	@Mapping(target = "hasReviewNote", ignore = true)
	@Mapping(target = "finalDecision", ignore = true)
	DocumentResponseDTO toDto(Document document);

	List<DocumentResponseDTO> toDtoList(List<Document> documents);
}
