package com.academic.integrity.review.dto;

import com.academic.integrity.review.domain.AnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisStatusDTO {
	private Long analysisId;
	private AnalysisStatus analysisStatus;
	private String errorMessage;
}