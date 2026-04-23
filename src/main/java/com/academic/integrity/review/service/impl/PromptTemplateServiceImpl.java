package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.domain.ApplicationSettings;
import com.academic.integrity.review.config.OpenAiProperties;
import com.academic.integrity.review.repository.ApplicationSettingsRepository;
import com.academic.integrity.review.service.PromptTemplateService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class PromptTemplateServiceImpl implements PromptTemplateService {

	private static final String PLACEHOLDER = "{{DOCUMENT_TEXT}}";
	private final OpenAiProperties openAiProperties;
	private final ApplicationSettingsRepository applicationSettingsRepository;
	private final String promptTemplate;

	public PromptTemplateServiceImpl(
			OpenAiProperties openAiProperties,
			ApplicationSettingsRepository applicationSettingsRepository) {
		this.openAiProperties = openAiProperties;
		this.applicationSettingsRepository = applicationSettingsRepository;
		this.promptTemplate = loadTemplate();
	}

	@Override
	public String buildPrompt(String documentText, Long userId) {
		if (documentText == null || documentText.isBlank()) {
			throw new IllegalArgumentException("Document text is required for analysis prompt");
		}
		if (userId == null) {
			throw new IllegalArgumentException("userId is required for analysis prompt");
		}

		String trimmed = documentText.trim();
		int maxLength = openAiProperties.getMaxDocumentCharacters();
		String boundedText = trimmed.length() > maxLength
				? trimmed.substring(0, maxLength)
				: trimmed;

		String prompt = promptTemplate.replace(PLACEHOLDER, boundedText);
		return prompt
				+ buildOutputContractInstructions()
				+ buildModuleInstructions(userId);
	}

	private String buildOutputContractInstructions() {
		return """

		Output contract:
		- Return at most %d findings.
		- Rank findings by severity and evidentiary value, and keep only the strongest findings if more issues are visible.
		- Keep title at or below %d characters.
		- Keep explanation at or below %d characters.
		- Keep excerpt at or below %d characters.
		- Keep paragraphLocation at or below %d characters.
		- Keep suggestedAction at or below %d characters.
		- Use a short exact excerpt or short clause from the document, not a long passage.
		- Escape all JSON strings correctly.
		- Replace line breaks inside field values with spaces.
		- If more than %d issues are visible, return only the most important %d.
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

	private String buildModuleInstructions(Long userId) {
		ApplicationSettings settings = applicationSettingsRepository.findByUserId(userId).orElse(null);
		if (settings == null) {
			return "";
		}

		StringBuilder instructions = new StringBuilder();
		if (settings.isCitationAnalysis()) {
			instructions.append("- Citation analysis is enabled: prioritize directly evidenced citation and attribution issues.\n");
		}
		if (settings.isReferenceValidation()) {
			instructions.append("- Reference validation is enabled: focus on incomplete, malformed, or unverifiable references visible in the document text.\n");
		}
		if (settings.isFactualConsistencyReview()) {
			instructions.append("- Factual consistency review is enabled: use OTHER only for clearly evidenced factual consistency concerns in the text.\n");
		}
		if (settings.isWritingStyleConsistency()) {
			instructions.append("- Writing style consistency is enabled: pay attention to directly visible paraphrasing and style inconsistency signals.\n");
		}
		if (settings.isAiReviewAssistance()) {
			instructions.append("- AI review assistance is enabled: consider AI_GENERATED_CONTENT only when the document shows strong visible heuristic signals.\n");
		}

		if (instructions.isEmpty()) {
			return "";
		}

		return "\n\nEnabled review modules:\n"
				+ instructions
				+ "Only return findings within enabled review modules when this section is present.\n";
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