package com.academic.integrity.review.exception;

import com.academic.integrity.review.config.ErrorHandlingProperties;
import com.academic.integrity.review.config.RequestIdFilter;
import com.academic.integrity.review.dto.ErrorDebugDetailsDTO;
import com.academic.integrity.review.dto.ErrorResponseDTO;
import com.academic.integrity.review.dto.ValidationErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ApiErrorResponseFactory {

	private final ErrorHandlingProperties errorHandlingProperties;

	public ErrorResponseDTO build(HttpServletRequest request, HttpStatus status, String message) {
		return build(request, status, message, null, null);
	}

	public ErrorResponseDTO build(HttpServletRequest request, HttpStatus status, String message, Throwable exception) {
		return build(request, status, message, exception, null);
	}

	public ErrorResponseDTO build(
			HttpServletRequest request,
			HttpStatus status,
			String message,
			Throwable exception,
			List<ValidationErrorDTO> validationErrors) {
		String resolvedMessage = StringUtils.hasText(message) ? message : status.getReasonPhrase();

		return ErrorResponseDTO.builder()
				.timestamp(Instant.now())
				.status(status.value())
				.error(status.getReasonPhrase())
				.message(resolvedMessage)
				.path(request.getRequestURI())
				.requestId(resolveRequestId(request))
				.validationErrors(validationErrors == null || validationErrors.isEmpty() ? null : validationErrors)
				.debugDetails(buildDebugDetails(exception))
				.build();
	}

	private ErrorDebugDetailsDTO buildDebugDetails(Throwable exception) {
		if (exception == null || !errorHandlingProperties.isIncludeDebugDetails()) {
			return null;
		}

		Throwable rootCause = rootCauseOf(exception);
		return new ErrorDebugDetailsDTO(
				exception.getClass().getName(),
				rootCause == null ? null : summarizeThrowable(rootCause),
				errorHandlingProperties.isIncludeStackTrace() ? stackTraceOf(exception) : null
		);
	}

	private static Throwable rootCauseOf(Throwable throwable) {
		Throwable current = throwable;
		while (current.getCause() != null && current.getCause() != current) {
			current = current.getCause();
		}
		return current;
	}

	private static String summarizeThrowable(Throwable throwable) {
		if (throwable == null) {
			return null;
		}
		String message = throwable.getMessage();
		return StringUtils.hasText(message)
				? throwable.getClass().getName() + ": " + message
				: throwable.getClass().getName();
	}

	private static String stackTraceOf(Throwable throwable) {
		StringWriter stringWriter = new StringWriter();
		throwable.printStackTrace(new PrintWriter(stringWriter));
		return stringWriter.toString();
	}

	private static String resolveRequestId(HttpServletRequest request) {
		Object requestId = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
		return requestId == null ? null : requestId.toString();
	}
}