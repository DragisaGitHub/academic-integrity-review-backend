package com.academic.integrity.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.academic.integrity.review.domain.ApplicationSettings;
import com.academic.integrity.review.domain.ColorTheme;
import com.academic.integrity.review.domain.DisplayDensity;
import com.academic.integrity.review.domain.ReadingLayout;
import com.academic.integrity.review.repository.ApplicationSettingsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationSettingsControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ApplicationSettingsRepository applicationSettingsRepository;

	@AfterEach
	void tearDown() {
		applicationSettingsRepository.deleteAll();
	}

	@Test
	void getSettingsCreatesFrontendCompatibleDefaults() throws Exception {
		String response = mockMvc.perform(get("/api/settings"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode json = objectMapper.readTree(response);
		assertThat(json.path("displayDensity").asText()).isEqualTo(DisplayDensity.COMFORTABLE.name());
		assertThat(json.path("readingLayout").asText()).isEqualTo(ReadingLayout.DEFAULT.name());
	}

	@Test
	void postSettingsAcceptsFrontendEnumValues() throws Exception {
		String request = """
				{
				  "professorName": "Dr. Stone",
				  "department": "History",
				  "university": "Example University",
				  "email": "prof@example.edu",
				  "citationAnalysis": true,
				  "referenceValidation": true,
				  "factualConsistencyReview": false,
				  "writingStyleConsistency": true,
				  "aiReviewAssistance": false,
				  "localAiEnabled": false,
				  "documentRetentionDays": 30,
				  "autoDeleteReviewedDocuments": false,
				  "storageLocation": "vault",
				  "colorTheme": "SYSTEM",
				  "displayDensity": "SPACIOUS",
				  "showSeverityBadges": true,
				  "readingLayout": "WIDE"
				}
				""";

		mockMvc.perform(post("/api/settings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
				.andExpect(status().isOk());

		ApplicationSettings saved = applicationSettingsRepository.findTopByOrderByIdAsc().orElseThrow();
		assertThat(saved.getColorTheme()).isEqualTo(ColorTheme.SYSTEM);
		assertThat(saved.getDisplayDensity()).isEqualTo(DisplayDensity.SPACIOUS);
		assertThat(saved.getReadingLayout()).isEqualTo(ReadingLayout.WIDE);
	}

	@Test
	void postSettingsRejectsMissingRequiredEnums() throws Exception {
		String request = """
				{
				  "professorName": "Dr. Stone",
				  "department": "History",
				  "university": "Example University",
				  "email": "prof@example.edu",
				  "citationAnalysis": true,
				  "referenceValidation": true,
				  "factualConsistencyReview": false,
				  "writingStyleConsistency": true,
				  "aiReviewAssistance": false,
				  "localAiEnabled": false,
				  "documentRetentionDays": 30,
				  "autoDeleteReviewedDocuments": false,
				  "storageLocation": "vault",
				  "showSeverityBadges": true
				}
				""";

		mockMvc.perform(post("/api/settings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
				.andExpect(status().isBadRequest());
	}
}