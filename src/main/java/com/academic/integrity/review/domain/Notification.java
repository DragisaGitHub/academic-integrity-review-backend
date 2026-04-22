package com.academic.integrity.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

	@Id
	@Column(name = "id", nullable = false, updatable = false, length = 36)
	private String id;

	@Column(name = "type", nullable = false, length = 64)
	private String type;

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "message", nullable = false, length = 512)
	private String message;

	@Column(name = "severity", nullable = false, length = 32)
	private String severity;

	@Column(name = "is_read", nullable = false)
	private boolean read;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "document_id")
	private Long documentId;

	@Column(name = "analysis_id")
	private Long analysisId;

	@Column(name = "route", nullable = false, length = 255)
	private String route;

	@PrePersist
	void onCreate() {
		if (id == null || id.isBlank()) {
			id = UUID.randomUUID().toString();
		}
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}