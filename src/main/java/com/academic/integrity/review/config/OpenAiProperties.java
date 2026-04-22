package com.academic.integrity.review.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {

	private String apiKey = "";
	private String model = "gpt-4o-mini";
	private int maxTokens = 4096;
	private double temperature = 0.0;
	private int maxDocumentCharacters = 60000;
}