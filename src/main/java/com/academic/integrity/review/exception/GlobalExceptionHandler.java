package com.academic.integrity.review.exception;

import com.academic.integrity.review.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(DuplicateAnalysisException.class)
	public ResponseEntity<ErrorResponseDTO> handleDuplicateAnalysisException(DuplicateAnalysisException ex, HttpServletRequest request) {
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
		return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponseDTO> handleException(Exception ex, HttpServletRequest request) {
		return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
	}

	private ResponseEntity<ErrorResponseDTO> buildErrorResponse(HttpStatus status, Exception ex, HttpServletRequest request) {
		String message = ex.getMessage();
		if (message == null || message.isBlank()) {
			message = status.getReasonPhrase();
		}

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
