package com.academic.integrity.review.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {
	private String id;
	private String type;
	private String title;
	private String message;
	private String severity;
	private boolean read;
	private Instant createdAt;
	private Long documentId;
	private Long analysisId;
	private String route;
}