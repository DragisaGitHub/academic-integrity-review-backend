package com.academic.integrity.review.dto;

import com.academic.integrity.review.domain.FinalDecision;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewNoteResponseDTO {
	private Long id;
	private Long documentId;
	private String notes;
	private boolean referencesChecked;
	private boolean oralDefenseRequired;
	private boolean factualIssuesDiscussed;
	private boolean finalReviewCompleted;
	private FinalDecision finalDecision;
	private Instant createdAt;
	private Instant updatedAt;
}
