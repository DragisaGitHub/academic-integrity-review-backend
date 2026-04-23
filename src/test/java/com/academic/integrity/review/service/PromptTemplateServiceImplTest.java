package com.academic.integrity.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.academic.integrity.review.config.OpenAiProperties;
import com.academic.integrity.review.repository.ApplicationSettingsRepository;
import com.academic.integrity.review.service.impl.PromptTemplateServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromptTemplateServiceImplTest {

	@Mock
	private ApplicationSettingsRepository applicationSettingsRepository;

	private PromptTemplateService promptTemplateService;

	@BeforeEach
	void setUp() {
		OpenAiProperties openAiProperties = new OpenAiProperties();
		openAiProperties.setMaxFindings(8);
		openAiProperties.setMaxTitleCharacters(120);
		openAiProperties.setMaxExplanationCharacters(320);
		openAiProperties.setMaxExcerptCharacters(220);
		openAiProperties.setMaxParagraphLocationCharacters(120);
		openAiProperties.setMaxSuggestedActionCharacters(180);
		openAiProperties.setMaxDocumentCharacters(25_000);

		promptTemplateService = new PromptTemplateServiceImpl(openAiProperties, applicationSettingsRepository);
	}

	@Test
	void buildPromptAppendsBoundedOutputContract() {
		when(applicationSettingsRepository.findByUserId(7L)).thenReturn(Optional.empty());

		String prompt = promptTemplateService.buildPrompt("Document text", 7L);

		assertThat(prompt).contains("Return at most 8 findings.");
		assertThat(prompt).contains("Keep title at or below 120 characters.");
		assertThat(prompt).contains("Keep explanation at or below 320 characters.");
		assertThat(prompt).contains("Keep excerpt at or below 220 characters.");
		assertThat(prompt).contains("Replace line breaks inside field values with spaces.");
		assertThat(prompt).contains("If more than 8 issues are visible");
	}
}