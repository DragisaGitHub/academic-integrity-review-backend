package com.academic.integrity.review.exception;

import com.academic.integrity.review.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(DuplicateAnalysisException.class)
	public ResponseEntity<ErrorResponseDTO> handleDuplicateAnalysisException(DuplicateAnalysisException ex, HttpServletRequest request) {
		return buildErrorResponse(HttpStatus.CONFLICT, ex, request);
	}

	@ExceptionHandler(AnalysisRetryNotAllowedException.class)
	public ResponseEntity<ErrorResponseDTO> handleAnalysisRetryNotAllowed(AnalysisRetryNotAllowedException ex, HttpServletRequest request) {
		return buildErrorResponse(HttpStatus.CONFLICT, ex, request);
	}

	@ExceptionHandler(DocumentDeletionNotAllowedException.class)
	public ResponseEntity<ErrorResponseDTO> handleDocumentDeletionNotAllowed(DocumentDeletionNotAllowedException ex, HttpServletRequest request) {
		return buildErrorResponse(HttpStatus.CONFLICT, ex, request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
		return buildErrorResponse(HttpStatus.BAD_REQUEST, ex, request);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponseDTO> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		return buildErrorResponse(HttpStatus.NOT_FOUND, ex, request);
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ErrorResponseDTO> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
		return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponseDTO> handleException(Exception ex, HttpServletRequest request) {
		return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	@ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
	public ErrorResponseDTO handleMaxUploadSizeExceeded(
			MaxUploadSizeExceededException ex,
			HttpServletRequest request) {
		return new ErrorResponseDTO(
				Instant.now(),
				HttpStatus.PAYLOAD_TOO_LARGE.value(),
				"Payload Too Large",
				"Uploaded file exceeds the maximum allowed size.",
				request.getRequestURI()
		);
	}

	private ResponseEntity<ErrorResponseDTO> buildErrorResponse(HttpStatus status, Exception ex, HttpServletRequest request) {
		String message = ex.getMessage();
		if (message == null || message.isBlank()) {
			message = status.getReasonPhrase();
		}

		return buildErrorResponse(status, message, request);
	}

	private ResponseEntity<ErrorResponseDTO> buildErrorResponse(HttpStatus status, String message, HttpServletRequest request) {
		ErrorResponseDTO body = new ErrorResponseDTO(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				request.getRequestURI()
		);

		return ResponseEntity.status(status).body(body);
	}
}
