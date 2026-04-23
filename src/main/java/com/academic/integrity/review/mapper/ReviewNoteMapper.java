package com.academic.integrity.review.mapper;

import com.academic.integrity.review.domain.ReviewNote;
import com.academic.integrity.review.dto.ReviewNoteResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = DocumentMapper.class)
public interface ReviewNoteMapper {

	ReviewNoteResponseDTO toDto(ReviewNote reviewNote);
}
