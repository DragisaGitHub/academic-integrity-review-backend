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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "review_notes")
@Getter
@Setter
@NoArgsConstructor
public class ReviewNote {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "document_id", nullable = false, unique = true)
	private Document document;

	@Column(name = "notes", columnDefinition = "LONGTEXT")
	private String notes;

	@Column(name = "references_checked", nullable = false)
	private boolean referencesChecked;

	@Column(name = "oral_defense_required", nullable = false)
	private boolean oralDefenseRequired;

	@Column(name = "factual_issues_discussed", nullable = false)
	private boolean factualIssuesDiscussed;

	@Column(name = "final_review_completed", nullable = false)
	private boolean finalReviewCompleted;

	@Enumerated(EnumType.STRING)
	@Column(name = "final_decision")
	private FinalDecision finalDecision;

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
