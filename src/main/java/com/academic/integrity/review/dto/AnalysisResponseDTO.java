package com.academic.integrity.review.dto;

import com.academic.integrity.review.domain.AnalysisStatus;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponseDTO {
	private Long id;
	private DocumentRefDTO document;
	private LocalDate analysisDate;
	private AnalysisStatus analysisStatus;
	private String errorMessage;
	private String fullText;
	private Instant createdAt;
	private Instant updatedAt;
}
