package com.academic.integrity.review.dto;

import com.academic.integrity.review.domain.FindingCategory;
import com.academic.integrity.review.domain.FindingSeverity;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindingResponseDTO {
	private Long id;
	private Long analysisId;
	private FindingCategory category;
	private FindingSeverity severity;
	private String title;
	private String explanation;
	private String excerpt;
	private String paragraphLocation;
	private String suggestedAction;
	private String professorNotes;
	private boolean reviewed;
	private boolean flaggedForFollowUp;
	private Instant createdAt;
	private Instant updatedAt;
}
