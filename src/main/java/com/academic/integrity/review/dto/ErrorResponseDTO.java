package com.academic.integrity.review.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDTO {
	private Instant timestamp;
	private int status;
	private String error;
	private String message;
	private String path;
}
