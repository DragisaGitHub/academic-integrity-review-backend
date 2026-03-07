package com.academic.integrity.review.mapper;

import com.academic.integrity.review.domain.ReviewNote;
import com.academic.integrity.review.dto.ReviewNoteResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewNoteMapper {

	@Mapping(target = "documentId", source = "document.id")
	ReviewNoteResponseDTO toDto(ReviewNote reviewNote);
}
