package com.academic.integrity.review.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {

	private String apiKey = "";
	private String model = "gpt-4o-mini";
	private int maxTokens = 4096;
	private double temperature = 0.2;
	private int maxDocumentCharacters = 60000;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public int getMaxTokens() {
		return maxTokens;
	}

	public void setMaxTokens(int maxTokens) {
		this.maxTokens = maxTokens;
	}

	public double getTemperature() {
		return temperature;
	}

	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}

	public int getMaxDocumentCharacters() {
		return maxDocumentCharacters;
	}

	public void setMaxDocumentCharacters(int maxDocumentCharacters) {
		this.maxDocumentCharacters = maxDocumentCharacters;
	}
}