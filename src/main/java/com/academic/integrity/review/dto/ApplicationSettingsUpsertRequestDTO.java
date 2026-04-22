package com.academic.integrity.review.dto;

import com.academic.integrity.review.domain.ColorTheme;
import com.academic.integrity.review.domain.DisplayDensity;
import com.academic.integrity.review.domain.ReadingLayout;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSettingsUpsertRequestDTO {
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

	// Data retention
	private int documentRetentionDays;
	private boolean autoDeleteReviewedDocuments;

	private String storageLocation;

	// Interface preferences
	@NotNull(message = "colorTheme is required")
	private ColorTheme colorTheme;
	@NotNull(message = "displayDensity is required")
	private DisplayDensity displayDensity;
	private boolean showSeverityBadges;
	@NotNull(message = "readingLayout is required")
	private ReadingLayout readingLayout;
}
