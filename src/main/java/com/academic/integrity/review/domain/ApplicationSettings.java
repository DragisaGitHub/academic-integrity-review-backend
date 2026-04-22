package com.academic.integrity.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "application_settings")
@Getter
@Setter
@NoArgsConstructor
public class ApplicationSettings {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "professor_name")
	private String professorName;

	@Column(name = "department")
	private String department;

	@Column(name = "university")
	private String university;

	@Column(name = "email")
	private String email;

	@Column(name = "citation_analysis", nullable = false)
	private boolean citationAnalysis;

	@Column(name = "reference_validation", nullable = false)
	private boolean referenceValidation;

	@Column(name = "factual_consistency_review", nullable = false)
	private boolean factualConsistencyReview;

	@Column(name = "writing_style_consistency", nullable = false)
	private boolean writingStyleConsistency;

	@Column(name = "ai_review_assistance", nullable = false)
	private boolean aiReviewAssistance;

	@Column(name = "local_ai_enabled", nullable = false)
	private boolean localAiEnabled;

	@Column(name = "document_retention_days", nullable = false)
	private int documentRetentionDays;

	@Column(name = "auto_delete_reviewed_documents", nullable = false)
	private boolean autoDeleteReviewedDocuments;

	@Column(name = "storage_location")
	private String storageLocation;

	@Enumerated(EnumType.STRING)
	@Column(name = "color_theme", nullable = false)
	private ColorTheme colorTheme = ColorTheme.DARK;

	@Enumerated(EnumType.STRING)
	@Column(name = "display_density", nullable = false)
	private DisplayDensity displayDensity = DisplayDensity.COMFORTABLE;

	@Column(name = "show_severity_badges", nullable = false)
	private boolean showSeverityBadges = true;

	@Column(name = "light_theme_enabled", nullable = false)
	private boolean lightThemeEnabled;

	@Enumerated(EnumType.STRING)
	@Column(name = "reading_layout", nullable = false)
	private ReadingLayout readingLayout = ReadingLayout.DEFAULT;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}
}
