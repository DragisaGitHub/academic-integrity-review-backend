package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.config.OpenAiProperties;
import com.academic.integrity.review.service.PromptTemplateService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class PromptTemplateServiceImpl implements PromptTemplateService {

	private static final String PLACEHOLDER = "{{DOCUMENT_TEXT}}";
	private final OpenAiProperties openAiProperties;
	private final String promptTemplate;

	public PromptTemplateServiceImpl(OpenAiProperties openAiProperties) {
		this.openAiProperties = openAiProperties;
		this.promptTemplate = loadTemplate();
	}

	@Override
	public String buildPrompt(String documentText) {
		if (documentText == null || documentText.isBlank()) {
			throw new IllegalArgumentException("Document text is required for analysis prompt");
		}

		String trimmed = documentText.trim();
		int maxLength = openAiProperties.getMaxDocumentCharacters();
		String boundedText = trimmed.length() > maxLength
				? trimmed.substring(0, maxLength)
				: trimmed;

		return promptTemplate.replace(PLACEHOLDER, boundedText);
	}

	private static String loadTemplate() {
		ClassPathResource resource = new ClassPathResource("prompts/analysis-prompt.txt");
		try {
			byte[] bytes = resource.getInputStream().readAllBytes();
			return new String(bytes, StandardCharsets.UTF_8);
		} catch (IOException ex) {
			throw new IllegalStateException("Failed to load analysis prompt template", ex);
		}
	}
}