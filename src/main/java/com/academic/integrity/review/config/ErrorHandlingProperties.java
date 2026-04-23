package com.academic.integrity.review.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.errors")
public class ErrorHandlingProperties {

	private boolean includeDebugDetails = false;
	private boolean includeStackTrace = false;
	private String requestIdHeader = "X-Request-Id";
}