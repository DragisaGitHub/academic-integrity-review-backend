package com.academic.integrity.review.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAnalysisRequestDTO {
	private Long documentId;
	private String fullText;
}
