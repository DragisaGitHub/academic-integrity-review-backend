package com.academic.integrity.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
public class Document {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "student_name", nullable = false)
	private String studentName;

	@Column(name = "course", nullable = false)
	private String course;

	@Column(name = "academic_year")
	private String academicYear;

	@Column(name = "submission_date")
	private LocalDate submissionDate;

	@Column(name = "original_filename")
	private String originalFilename;

	@Column(name = "stored_filename")
	private String storedFilename;

	@Column(name = "stored_path")
	private String storedPath;

	@Column(name = "content_type")
	private String contentType;

	@Column(name = "file_size")
	private Long fileSize;

	@Enumerated(EnumType.STRING)
	@Column(name = "review_priority", nullable = false)
	private ReviewPriority reviewPriority = ReviewPriority.MEDIUM;

	@Enumerated(EnumType.STRING)
	@Column(name = "review_status", nullable = false)
	private ReviewStatus reviewStatus = ReviewStatus.PENDING;

	@OneToOne(mappedBy = "document", fetch = FetchType.LAZY)
	private Analysis analysis;

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
