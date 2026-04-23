package com.academic.integrity.review.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RequestIdFilter extends OncePerRequestFilter {

	public static final String REQUEST_ID_ATTRIBUTE = RequestIdFilter.class.getName() + ".REQUEST_ID";

	private final ErrorHandlingProperties errorHandlingProperties;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String headerName = errorHandlingProperties.getRequestIdHeader();
		String requestId = request.getHeader(headerName);
		if (!StringUtils.hasText(requestId)) {
			requestId = UUID.randomUUID().toString();
		}

		request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
		response.setHeader(headerName, requestId);
		filterChain.doFilter(request, response);
	}
}