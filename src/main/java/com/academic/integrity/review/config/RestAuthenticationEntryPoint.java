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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;
	private final ApiErrorResponseFactory apiErrorResponseFactory;

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		log.warn("Authentication failed for {} {} [requestId={}]: {}",
				request.getMethod(),
				request.getRequestURI(),
				request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE),
				authException.getMessage());
		writeError(request, response, authException);
	}

	private void writeError(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
			throws IOException {
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		ErrorResponseDTO body = apiErrorResponseFactory.build(
				request,
				HttpStatus.UNAUTHORIZED,
				"Authentication is required to access this resource.",
				authException
		);
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}