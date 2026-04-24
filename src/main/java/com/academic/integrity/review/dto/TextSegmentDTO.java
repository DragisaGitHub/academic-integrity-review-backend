package com.academic.integrity.review.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextSegmentDTO {
	private Long id;
	private Integer segmentIndex;
	private String content;
	private Integer startOffset;
	private Integer endOffset;
}