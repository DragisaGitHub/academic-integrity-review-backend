package com.academic.integrity.review.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisNotesResponseDTO {
	private Long analysisId;
	private String notes;
	private Instant updatedAt;
}