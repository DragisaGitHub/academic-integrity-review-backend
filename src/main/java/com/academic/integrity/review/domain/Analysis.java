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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "analyses")
@Getter
@Setter
@NoArgsConstructor
public class Analysis {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "document_id", nullable = false, unique = true)
	private Document document;

	@Column(name = "analysis_date")
	private LocalDate analysisDate;

	@Column(name = "full_text", columnDefinition = "LONGTEXT")
	private String fullText;

	@Enumerated(EnumType.STRING)
	@Column(name = "analysis_status", nullable = false)
	private AnalysisStatus analysisStatus = AnalysisStatus.PENDING;

	@Column(name = "error_message", columnDefinition = "LONGTEXT")
	private String errorMessage;

	@Column(name = "analysis_notes", columnDefinition = "LONGTEXT")
	private String analysisNotes;

	@OneToMany(mappedBy = "analysis")
	private List<Finding> findings = new ArrayList<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "model_name")
	private String modelName;

	@Column(name = "total_tokens_used")
	private Integer totalTokensUsed;

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
