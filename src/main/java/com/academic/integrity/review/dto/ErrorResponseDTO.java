package com.academic.integrity.review.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDTO {
	private Instant timestamp;
	private int status;
	private String error;
	private String message;
	private String path;
	private String requestId;
	private java.util.List<ValidationErrorDTO> validationErrors;
	private ErrorDebugDetailsDTO debugDetails;
}
