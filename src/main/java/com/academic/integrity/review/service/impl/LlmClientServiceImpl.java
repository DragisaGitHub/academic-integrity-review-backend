package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.config.OpenAiProperties;
import com.academic.integrity.review.service.LlmClientService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class LlmClientServiceImpl implements LlmClientService {

	private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

	private final OpenAiProperties openAiProperties;
	private final RestClient restClient;

	public LlmClientServiceImpl(OpenAiProperties openAiProperties, RestClient.Builder restClientBuilder) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(15000);
		requestFactory.setReadTimeout(90000);

		this.openAiProperties = openAiProperties;
		this.restClient = restClientBuilder
				.requestFactory(requestFactory)
				.build();
	}

	@Override
	public LlmAnalysisResult analyze(String prompt) {
		if (!StringUtils.hasText(openAiProperties.getApiKey())) {
			throw new IllegalStateException("OpenAI API key is not configured");
		}

		Map<String, Object> requestBody = Map.of(
				"model", openAiProperties.getModel(),
				"response_format", Map.of("type", "json_object"),
				"messages", List.of(Map.of("role", "user", "content", prompt)),
				"temperature", openAiProperties.getTemperature(),
				"max_tokens", openAiProperties.getMaxTokens()
		);

		JsonNode response = restClient.post()
				.uri(OPENAI_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + openAiProperties.getApiKey())
				.body(requestBody)
				.retrieve()
				.body(JsonNode.class);

		if (response == null) {
			throw new IllegalStateException("OpenAI returned an empty response");
		}

		String content = response.path("choices").path(0).path("message").path("content").asText(null);
		if (!StringUtils.hasText(content)) {
			throw new IllegalStateException("OpenAI response did not contain analysis content");
		}

		String model = response.path("model").asText(openAiProperties.getModel());
		Integer totalTokens = response.path("usage").path("total_tokens").isMissingNode()
				? null
				: response.path("usage").path("total_tokens").asInt();

		return new LlmAnalysisResult(content, model, totalTokens);
	}
}