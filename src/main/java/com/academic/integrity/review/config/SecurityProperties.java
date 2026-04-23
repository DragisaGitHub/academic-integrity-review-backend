package com.academic.integrity.review.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

	private String jwtSecret = "local-dev-jwt-secret-change-me-1234567890";
	private long jwtExpirationHours = 24;
}