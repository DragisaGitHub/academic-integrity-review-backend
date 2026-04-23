package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.config.OpenAiProperties;
import com.academic.integrity.review.exception.AiFindingsResponseException;
import com.academic.integrity.review.service.LlmClientService;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class LlmClientServiceImpl implements LlmClientService {

	private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
	private static final Logger log = LoggerFactory.getLogger(LlmClientServiceImpl.class);

	private final OpenAiProperties openAiProperties;
	private final RestClient restClient;

	public LlmClientServiceImpl(OpenAiProperties openAiProperties, RestClient.Builder restClientBuilder) {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(30))
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofSeconds(240));

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
						Map.of("role", "developer", "content", buildDeveloperMessage()),
						Map.of("role", "user", "content", prompt)
				),
				"temperature", openAiProperties.getTemperature(),
				"max_tokens", openAiProperties.getMaxTokens());

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

		String responseId = nullableText(response.path("id"));
		JsonNode choice = response.path("choices").path(0);
		String finishReason = nullableText(choice.path("finish_reason"));
		String refusal = nullableText(choice.path("message").path("refusal"));

		if (StringUtils.hasText(refusal)) {
			throw new AiFindingsResponseException(
					AiFindingsResponseException.Kind.REFUSED_RESPONSE,
					"AI refused to generate findings for this document. Please retry the analysis.",
					"OpenAI refused findings response. responseId=%s model=%s refusal=%s"
							.formatted(responseId, openAiProperties.getModel(), refusal));
		}

		if (StringUtils.hasText(finishReason) && !"stop".equalsIgnoreCase(finishReason)) {
			AiFindingsResponseException.Kind kind = "length".equalsIgnoreCase(finishReason)
					? AiFindingsResponseException.Kind.INCOMPLETE_RESPONSE
					: AiFindingsResponseException.Kind.REFUSED_RESPONSE;
			String publicMessage = "length".equalsIgnoreCase(finishReason)
					? "AI returned an incomplete findings payload. Please retry the analysis."
					: "AI could not produce a valid findings payload. Please retry the analysis.";
			throw new AiFindingsResponseException(
					kind,
					publicMessage,
					"OpenAI response did not finish cleanly. responseId=%s model=%s finishReason=%s"
							.formatted(responseId, openAiProperties.getModel(), finishReason));
		}

		String content = nullableText(choice.path("message").path("content"));
		if (!StringUtils.hasText(content)) {
			throw new AiFindingsResponseException(
					AiFindingsResponseException.Kind.EMPTY_RESPONSE,
					"AI returned an empty findings payload. Please retry the analysis.",
					"OpenAI response did not contain findings content. responseId=%s model=%s"
							.formatted(responseId, openAiProperties.getModel()));
		}

		String model = response.path("model").asText(openAiProperties.getModel());
		Integer totalTokens = response.path("usage").path("total_tokens").isMissingNode()
				? null
				: response.path("usage").path("total_tokens").asInt();

		log.debug("OpenAI findings response received. responseId={} model={} finishReason={} totalTokens={}",
				responseId,
				model,
				finishReason,
				totalTokens);

		return new LlmAnalysisResult(content, model, totalTokens, finishReason, responseId);
	}

	private String buildDeveloperMessage() {
		return """
				You are a strict academic integrity auditor.
				Return only clearly evidence-based findings grounded in the provided document text.
				Be conservative: do not speculate, infer hidden facts, invent missing evidence, or overstate certainty.
				Only produce a finding when the issue is directly supported by the document text and a concrete excerpt.
				Use PLAGIARISM only for directly observable plagiarism signals in the text itself.
				Use AI_GENERATED_CONTENT only for strong visible heuristic suspicion, describe it as suspicion rather than proof, and keep severity at LOW or MEDIUM.
				Use CITATION_ISSUE, PARAPHRASING, and OTHER only when the concern is directly visible in the provided text.
				Return JSON that strictly matches the provided schema and contains no markdown, commentary, or extra fields.
				Return at most %d findings.
				Keep title at or below %d characters.
				Keep explanation at or below %d characters.
				Keep excerpt at or below %d characters and prefer a short exact phrase or sentence.
				Keep paragraphLocation at or below %d characters.
				Keep suggestedAction at or below %d characters.
				Escape all JSON strings correctly and replace line breaks inside values with spaces.
				If more than %d issues are visible, keep only the strongest %d findings.
				Do not return duplicate findings.
				""".formatted(
				openAiProperties.getMaxFindings(),
				openAiProperties.getMaxTitleCharacters(),
				openAiProperties.getMaxExplanationCharacters(),
				openAiProperties.getMaxExcerptCharacters(),
				openAiProperties.getMaxParagraphLocationCharacters(),
				openAiProperties.getMaxSuggestedActionCharacters(),
				openAiProperties.getMaxFindings(),
				openAiProperties.getMaxFindings());
	}

	private Map<String, Object> buildStructuredOutputResponseFormat() {
		return Map.of(
				"type", "json_schema",
				"json_schema", Map.of(
						"name", "academic_integrity_findings",
						"strict", true,
						"schema", buildFindingsSchema()));
	}

	private Map<String, Object> buildFindingsSchema() {
		return Map.of(
				"type", "object",
				"properties", Map.of(
						"findings", Map.of(
								"type", "array",
								"maxItems", openAiProperties.getMaxFindings(),
								"items", buildFindingItemSchema())),
				"required", List.of("findings"),
				"additionalProperties", false);
	}

	private Map<String, Object> buildFindingItemSchema() {
		return Map.of(
				"type", "object",
				"properties", Map.ofEntries(
						Map.entry("category", enumField("PLAGIARISM", "AI_GENERATED_CONTENT", "CITATION_ISSUE", "PARAPHRASING", "OTHER")),
						Map.entry("severity", enumField("LOW", "MEDIUM", "HIGH", "CRITICAL")),
						Map.entry("title", boundedStringField(openAiProperties.getMaxTitleCharacters())),
						Map.entry("explanation", boundedStringField(openAiProperties.getMaxExplanationCharacters())),
						Map.entry("excerpt", boundedStringField(openAiProperties.getMaxExcerptCharacters())),
						Map.entry("paragraphLocation", boundedStringField(openAiProperties.getMaxParagraphLocationCharacters())),
						Map.entry("suggestedAction", boundedStringField(openAiProperties.getMaxSuggestedActionCharacters()))),
				"required", List.of(
						"category",
						"severity",
						"title",
						"explanation",
						"excerpt",
						"paragraphLocation",
						"suggestedAction"),
				"additionalProperties", false);
	}

	private static Map<String, Object> enumField(String... values) {
		return Map.of("type", "string", "enum", List.of(values));
	}

	private static Map<String, Object> boundedStringField(int maxLength) {
		return Map.of("type", "string", "maxLength", maxLength);
	}

	private static String nullableText(JsonNode node) {
		if (node == null || node.isMissingNode() || node.isNull()) {
			return null;
		}
		return node.asText(null);
	}
}