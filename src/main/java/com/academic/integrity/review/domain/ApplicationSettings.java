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

	@Column(name = "local_ai_enabled", nullable = false)
	private boolean localAiEnabled;

	@Column(name = "document_retention_days", nullable = false)
	private int documentRetentionDays;

	@Column(name = "storage_location")
	private String storageLocation;

	@Column(name = "light_theme_enabled", nullable = false)
	private boolean lightThemeEnabled;

	@Enumerated(EnumType.STRING)
	@Column(name = "reading_layout")
	private ReadingLayout readingLayout;

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
