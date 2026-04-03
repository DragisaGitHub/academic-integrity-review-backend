package com.academic.integrity.review.dto;

import com.academic.integrity.review.domain.AnalysisStatus;
import com.academic.integrity.review.domain.FinalDecision;
import com.academic.integrity.review.domain.ReviewPriority;
import com.academic.integrity.review.domain.ReviewStatus;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponseDTO {
	private Long id;
	private String title;
	private String studentName;
	private String course;
	private LocalDate submissionDate;
	private ReviewPriority reviewPriority;
	private ReviewStatus reviewStatus;
	private boolean hasAnalysis;
	private Long analysisId;
	private AnalysisStatus analysisStatus;
	private String analysisErrorMessage;
	private boolean hasReviewNote;
	private FinalDecision finalDecision;
	private Instant createdAt;
	private Instant updatedAt;
}
