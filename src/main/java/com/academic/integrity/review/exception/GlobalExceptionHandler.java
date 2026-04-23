package com.academic.integrity.review.exception;

import com.academic.integrity.review.config.RequestIdFilter;
import com.academic.integrity.review.dto.ErrorResponseDTO;
import com.academic.integrity.review.dto.ValidationErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final ApiErrorResponseFactory apiErrorResponseFactory;

	@ExceptionHandler(DuplicateAnalysisException.class)
	public ResponseEntity<ErrorResponseDTO> handleDuplicateAnalysisException(DuplicateAnalysisException ex, HttpServletRequest request) {
		return clientError(HttpStatus.CONFLICT, ex, request);
	}

	@ExceptionHandler(AnalysisRetryNotAllowedException.class)
	public ResponseEntity<ErrorResponseDTO> handleAnalysisRetryNotAllowed(AnalysisRetryNotAllowedException ex, HttpServletRequest request) {
		return clientError(HttpStatus.CONFLICT, ex, request);
	}

	@ExceptionHandler(DocumentDeletionNotAllowedException.class)
	public ResponseEntity<ErrorResponseDTO> handleDocumentDeletionNotAllowed(DocumentDeletionNotAllowedException ex, HttpServletRequest request) {
		return clientError(HttpStatus.CONFLICT, ex, request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
		return clientError(HttpStatus.BAD_REQUEST, ex, request);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorResponseDTO> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
		return clientError(HttpStatus.UNAUTHORIZED, ex, request, "Authentication failed.");
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponseDTO> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
		return clientError(HttpStatus.FORBIDDEN, ex, request, "You do not have permission to access this resource.");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<ValidationErrorDTO> validationErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toValidationError)
				.toList();
		log.warn("Validation failed for {} {} [requestId={}]: {}",
				request.getMethod(),
				request.getRequestURI(),
				requestId(request),
				validationErrors.stream()
						.map(error -> error.getField() + " " + error.getMessage())
						.collect(Collectors.joining(", ")));
		return ResponseEntity.badRequest().body(apiErrorResponseFactory.build(
				request,
				HttpStatus.BAD_REQUEST,
				"Request validation failed.",
				ex,
				validationErrors));
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<ErrorResponseDTO> handleBindException(BindException ex, HttpServletRequest request) {
		List<ValidationErrorDTO> validationErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toValidationError)
				.toList();
		log.warn("Binding failed for {} {} [requestId={}]: {}",
				request.getMethod(),
				request.getRequestURI(),
				requestId(request),
				validationErrors.stream()
						.map(error -> error.getField() + " " + error.getMessage())
						.collect(Collectors.joining(", ")));
		return ResponseEntity.badRequest().body(apiErrorResponseFactory.build(
				request,
				HttpStatus.BAD_REQUEST,
				"Request validation failed.",
				ex,
				validationErrors));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponseDTO> handleConstraintViolationException(
			ConstraintViolationException ex,
			HttpServletRequest request) {
		List<ValidationErrorDTO> validationErrors = ex.getConstraintViolations().stream()
				.map(violation -> new ValidationErrorDTO(
						violation.getPropertyPath().toString(),
						violation.getMessage(),
						stringifyRejectedValue(violation.getInvalidValue())))
				.toList();
		log.warn("Constraint violation for {} {} [requestId={}]: {}",
				request.getMethod(),
				request.getRequestURI(),
				requestId(request),
				validationErrors.stream()
						.map(error -> error.getField() + " " + error.getMessage())
						.collect(Collectors.joining(", ")));
		return ResponseEntity.badRequest().body(apiErrorResponseFactory.build(
				request,
				HttpStatus.BAD_REQUEST,
				"Request validation failed.",
				ex,
				validationErrors));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponseDTO> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex,
			HttpServletRequest request) {
		String message = malformedPayloadMessage(ex);
		log.warn("Malformed request body for {} {} [requestId={}]: {}",
				request.getMethod(),
				request.getRequestURI(),
				requestId(request),
				message);
		return ResponseEntity.badRequest().body(apiErrorResponseFactory.build(request, HttpStatus.BAD_REQUEST, message, ex));
	}

	@ExceptionHandler({HttpMessageConversionException.class, MethodArgumentTypeMismatchException.class,
			MissingServletRequestParameterException.class})
	public ResponseEntity<ErrorResponseDTO> handleBadRequest(Exception ex, HttpServletRequest request) {
		return clientError(HttpStatus.BAD_REQUEST, ex, request, "Request parameters are invalid.");
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorResponseDTO> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
		return clientError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex, request, "Unsupported content type.");
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponseDTO> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
		return clientError(HttpStatus.METHOD_NOT_ALLOWED, ex, request, "HTTP method is not supported for this endpoint.");
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponseDTO> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		return clientError(HttpStatus.NOT_FOUND, ex, request);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolationException(
			DataIntegrityViolationException ex,
			HttpServletRequest request) {
		String message = isDuplicateOrConstraintFailure(ex)
				? "Database constraint violation."
				: "Data integrity violation.";
		log.warn("Data integrity violation for {} {} [requestId={}]: {}",
				request.getMethod(),
				request.getRequestURI(),
				requestId(request),
				rootCauseMessage(ex));
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(apiErrorResponseFactory.build(request, HttpStatus.CONFLICT, message, ex));
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<ErrorResponseDTO> handleDataAccessException(DataAccessException ex, HttpServletRequest request) {
		log.error("Database access failure for {} {} [requestId={}]: {}",
				request.getMethod(),
				request.getRequestURI(),
				requestId(request),
				rootCauseMessage(ex),
				ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(apiErrorResponseFactory.build(request, HttpStatus.INTERNAL_SERVER_ERROR,
						"Database operation failed.", ex));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ErrorResponseDTO> handleMaxUploadSizeExceeded(
			MaxUploadSizeExceededException ex,
			HttpServletRequest request) {
		return clientError(HttpStatus.PAYLOAD_TOO_LARGE, ex, request, "Uploaded file exceeds the maximum allowed size.");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponseDTO> handleException(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception for {} {} [requestId={}]",
				request.getMethod(),
				request.getRequestURI(),
				requestId(request),
				ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(apiErrorResponseFactory.build(request, HttpStatus.INTERNAL_SERVER_ERROR,
						"An unexpected error occurred.", ex));
	}

	private ResponseEntity<ErrorResponseDTO> clientError(HttpStatus status, Exception ex, HttpServletRequest request) {
		return clientError(status, ex, request, ex.getMessage());
	}

	private ResponseEntity<ErrorResponseDTO> clientError(
			HttpStatus status,
			Exception ex,
			HttpServletRequest request,
			String message) {
		String resolvedMessage = org.springframework.util.StringUtils.hasText(message) ? message : status.getReasonPhrase();
		log.warn("Handled {} for {} {} [requestId={}]: {}",
				status.value(),
				request.getMethod(),
				request.getRequestURI(),
				requestId(request),
				resolvedMessage);
		return ResponseEntity.status(status)
				.body(apiErrorResponseFactory.build(request, status, resolvedMessage, ex));
	}

	private ValidationErrorDTO toValidationError(FieldError error) {
		String defaultMessage = error.getDefaultMessage();
		if (defaultMessage == null || defaultMessage.isBlank()) {
			defaultMessage = "is invalid";
		}
		return new ValidationErrorDTO(error.getField(), defaultMessage, stringifyRejectedValue(error.getRejectedValue()));
	}

	private static String stringifyRejectedValue(Object rejectedValue) {
		if (rejectedValue == null) {
			return null;
		}
		String value = rejectedValue.toString();
		return value.length() > 200 ? value.substring(0, 200) + "..." : value;
	}

	private static String malformedPayloadMessage(HttpMessageNotReadableException ex) {
		Throwable cause = ex.getMostSpecificCause();
		if (!org.springframework.util.StringUtils.hasText(cause.getMessage())) {
			return "Malformed JSON request.";
		}
		return "Malformed JSON request: " + cause.getMessage();
	}

	private static boolean isDuplicateOrConstraintFailure(DataIntegrityViolationException ex) {
		String message = rootCauseMessage(ex).toLowerCase();
		return message.contains("duplicate")
				|| message.contains("constraint")
				|| message.contains("unique")
				|| message.contains("foreign key");
	}

	private static String rootCauseMessage(Throwable throwable) {
		Throwable current = throwable;
		while (current.getCause() != null && current.getCause() != current) {
			current = current.getCause();
		}
		return current.getMessage() == null ? current.getClass().getName() : current.getMessage();
	}

	private static Object requestId(HttpServletRequest request) {
		return request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
	}
}
