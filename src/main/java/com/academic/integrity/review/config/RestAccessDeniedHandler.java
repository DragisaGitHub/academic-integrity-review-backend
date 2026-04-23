package com.academic.integrity.review.config;

import com.academic.integrity.review.exception.ApiErrorResponseFactory;
import com.academic.integrity.review.dto.ErrorResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;
	private final ApiErrorResponseFactory apiErrorResponseFactory;

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		log.warn("Access denied for {} {} [requestId={}]: {}",
				request.getMethod(),
				request.getRequestURI(),
				request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE),
				accessDeniedException.getMessage());
		response.setStatus(HttpStatus.FORBIDDEN.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		ErrorResponseDTO body = apiErrorResponseFactory.build(
				request,
				HttpStatus.FORBIDDEN,
				"You do not have permission to access this resource.",
				accessDeniedException
		);
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}