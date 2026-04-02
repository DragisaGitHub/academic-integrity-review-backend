package com.academic.integrity.review.dto;

import com.academic.integrity.review.domain.ColorTheme;
import com.academic.integrity.review.domain.DisplayDensity;
import com.academic.integrity.review.domain.ReadingLayout;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSettingsResponseDTO {
	private Long id;

	// Profile
	private String professorName;
	private String department;
	private String university;
	private String email;

	// Analysis modules
	private boolean citationAnalysis;
	private boolean referenceValidation;
	private boolean factualConsistencyReview;
	private boolean writingStyleConsistency;
	private boolean aiReviewAssistance;

	// Local processing
	private boolean localAiEnabled;
	private int documentRetentionDays;

	// Data retention
	private boolean autoDeleteReviewedDocuments;
	private String storageLocation;

	// Interface preferences
	private ColorTheme colorTheme;
	private DisplayDensity displayDensity;
	private boolean showSeverityBadges;
	private ReadingLayout readingLayout;

	private Instant createdAt;
	private Instant updatedAt;
}
