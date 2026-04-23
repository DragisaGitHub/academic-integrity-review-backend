package com.academic.integrity.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.academic.integrity.review.domain.ApplicationSettings;
import com.academic.integrity.review.domain.ColorTheme;
import com.academic.integrity.review.domain.DisplayDensity;
import com.academic.integrity.review.domain.ReadingLayout;
import com.academic.integrity.review.domain.User;
import com.academic.integrity.review.domain.UserRole;
import com.academic.integrity.review.repository.ApplicationSettingsRepository;
import com.academic.integrity.review.repository.UserRepository;
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

	@Autowired
	private UserRepository userRepository;

	@AfterEach
	void tearDown() {
		applicationSettingsRepository.deleteAll();
	}

	@Test
	void getSettingsCreatesFrontendCompatibleDefaults() throws Exception {
		ensureUser("user-a", UserRole.USER);

		String response = mockMvc.perform(get("/api/settings").with(user("user-a").roles("USER")))
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
		User userA = ensureUser("user-a", UserRole.USER);

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
				.with(user("user-a").roles("USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
				.andExpect(status().isOk());

		ApplicationSettings saved = applicationSettingsRepository.findByUserId(userA.getId()).orElseThrow();
		assertThat(saved.getColorTheme()).isEqualTo(ColorTheme.SYSTEM);
		assertThat(saved.getDisplayDensity()).isEqualTo(DisplayDensity.SPACIOUS);
		assertThat(saved.getReadingLayout()).isEqualTo(ReadingLayout.WIDE);
	}

	@Test
	void postSettingsRejectsMissingRequiredEnums() throws Exception {
		ensureUser("user-a", UserRole.USER);

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
				.with(user("user-a").roles("USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
				.andExpect(status().isBadRequest());
	}

	@Test
	void settingsAreIsolatedPerAuthenticatedUser() throws Exception {
		User userA = ensureUser("user-a", UserRole.USER);
		User userB = ensureUser("user-b", UserRole.USER);

		mockMvc.perform(post("/api/settings")
				.with(user("user-a").roles("USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(settingsRequest("Professor A", true, false, false)))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/settings")
				.with(user("user-b").roles("USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(settingsRequest("Professor B", false, true, true)))
				.andExpect(status().isOk());

		String userAResponse = mockMvc.perform(get("/api/settings").with(user("user-a").roles("USER")))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String userBResponse = mockMvc.perform(get("/api/settings").with(user("user-b").roles("USER")))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode userAJson = objectMapper.readTree(userAResponse);
		JsonNode userBJson = objectMapper.readTree(userBResponse);
		assertThat(userAJson.path("professorName").asText()).isEqualTo("Professor A");
		assertThat(userBJson.path("professorName").asText()).isEqualTo("Professor B");
		assertThat(userAJson.path("citationAnalysis").asBoolean()).isTrue();
		assertThat(userAJson.path("referenceValidation").asBoolean()).isFalse();
		assertThat(userBJson.path("citationAnalysis").asBoolean()).isFalse();
		assertThat(userBJson.path("referenceValidation").asBoolean()).isTrue();

		assertThat(applicationSettingsRepository.findByUserId(userA.getId())).isPresent();
		assertThat(applicationSettingsRepository.findByUserId(userB.getId())).isPresent();
	}

	private User ensureUser(String username, UserRole role) {
		return userRepository.findByUsernameIgnoreCase(username)
				.orElseGet(() -> {
					User user = new User();
					user.setUsername(username);
					user.setPasswordHash("test-hash");
					user.setDisplayName(username);
					user.setRole(role);
					user.setEnabled(true);
					return userRepository.saveAndFlush(user);
				});
	}

	private static String settingsRequest(
			String professorName,
			boolean citationAnalysis,
			boolean referenceValidation,
			boolean factualConsistencyReview) {
		return """
				{
				  "professorName": "%s",
				  "department": "History",
				  "university": "Example University",
				  "email": "prof@example.edu",
				  "citationAnalysis": %s,
				  "referenceValidation": %s,
				  "factualConsistencyReview": %s,
				  "writingStyleConsistency": false,
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
				""".formatted(professorName, citationAnalysis, referenceValidation, factualConsistencyReview);
	}
}