package com.academic.integrity.review.dto;

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
	private Long documentId;
	private LocalDate analysisDate;
	private String fullText;
	private Instant createdAt;
	private Instant updatedAt;
}
