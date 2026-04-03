package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.config.OpenAiProperties;
import com.academic.integrity.review.service.LlmClientService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class LlmClientServiceImpl implements LlmClientService {

	private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
	private static final String ACADEMIC_INTEGRITY_DEVELOPER_MESSAGE = """
			You are a strict academic integrity auditor.
			Return only clearly evidence-based findings grounded in the provided document text.
			Be conservative: do not speculate, infer hidden facts, invent missing evidence, or overstate certainty.
			Only produce a finding when the issue is directly supported by the document text and a concrete excerpt.
			Use PLAGIARISM only for directly observable plagiarism signals in the text itself.
			Use AI_GENERATED_CONTENT only for strong visible heuristic suspicion, describe it as suspicion rather than proof, and keep severity at LOW or MEDIUM.
			Use CITATION_ISSUE, PARAPHRASING, and OTHER only when the concern is directly visible in the provided text.
			Do not return duplicate findings. Do not add markdown, commentary, or extra fields.
			""";

	private final OpenAiProperties openAiProperties;
	private final RestClient restClient;

	public LlmClientServiceImpl(OpenAiProperties openAiProperties, RestClient.Builder restClientBuilder) {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(30))
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofSeconds(90));

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
				"response_format", buildStructuredOutputResponseFormat(),
				"messages", List.of(
						Map.of("role", "developer", "content", ACADEMIC_INTEGRITY_DEVELOPER_MESSAGE),
						Map.of("role", "user", "content", prompt)
				),
				"temperature", 0.0,
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

	private static Map<String, Object> buildStructuredOutputResponseFormat() {
		return Map.of(
				"type", "json_schema",
				"json_schema", Map.of(
						"name", "academic_integrity_findings",
						"strict", true,
						"schema", Map.of(
								"type", "object",
								"properties", Map.of(
										"findings", Map.of(
												"type", "array",
												"items", Map.of(
														"type", "object",
														"properties", Map.of(
																"category", Map.of(
																		"type", "string",
																		"enum", List.of("PLAGIARISM", "AI_GENERATED_CONTENT", "CITATION_ISSUE", "PARAPHRASING", "OTHER")
																),
																"severity", Map.of(
																		"type", "string",
																		"enum", List.of("LOW", "MEDIUM", "HIGH", "CRITICAL")
																),
																"title", Map.of("type", "string"),
																"explanation", Map.of("type", "string"),
																"excerpt", Map.of("type", "string"),
																"paragraphLocation", Map.of("type", "string"),
																"suggestedAction", Map.of("type", "string")
														),
														"required", List.of(
																"category",
																"severity",
																"title",
																"explanation",
																"excerpt",
																"paragraphLocation",
																"suggestedAction"
														),
														"additionalProperties", false
												)
										)
								),
								"required", List.of("findings"),
								"additionalProperties", false
						)
				)
		);
	}
}