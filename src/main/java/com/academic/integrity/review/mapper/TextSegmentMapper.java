package com.academic.integrity.review.mapper;

import com.academic.integrity.review.domain.TextSegment;
import com.academic.integrity.review.dto.TextSegmentDTO;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TextSegmentMapper {

	TextSegmentDTO toDto(TextSegment textSegment);

	List<TextSegmentDTO> toDtoList(List<TextSegment> textSegments);
}