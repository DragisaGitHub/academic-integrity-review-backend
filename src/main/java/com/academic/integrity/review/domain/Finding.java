package com.academic.integrity.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "findings")
@Getter
@Setter
@NoArgsConstructor
public class Finding {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "analysis_id", nullable = false)
	private Analysis analysis;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false)
	private FindingCategory category;

	@Enumerated(EnumType.STRING)
	@Column(name = "severity", nullable = false)
	private FindingSeverity severity;

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "explanation", columnDefinition = "LONGTEXT")
	private String explanation;

	@Column(name = "excerpt", columnDefinition = "LONGTEXT")
	private String excerpt;

	@Column(name = "paragraph_location")
	private String paragraphLocation;

	@Column(name = "suggested_action", columnDefinition = "LONGTEXT")
	private String suggestedAction;

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
