package com.academic.integrity.review.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisFullTextDTO {
	private Long analysisId;
	private String fullText;
}