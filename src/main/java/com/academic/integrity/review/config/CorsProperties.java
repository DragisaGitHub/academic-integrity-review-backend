package com.academic.integrity.review.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

	private List<String> allowedOrigins = List.of("http://localhost:5173");
	private long maxAge = 3600L;
}