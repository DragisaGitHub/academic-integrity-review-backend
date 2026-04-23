package com.academic.integrity.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.academic.integrity.review.domain.User;
import com.academic.integrity.review.domain.UserRole;
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
class ErrorHandlingIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@AfterEach
	void tearDown() {
		userRepository.deleteAll();
	}

	@Test
	void invalidLoginReturnsStructuredUnauthorizedError() throws Exception {
		String responseBody = mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "username": "missing-user",
						  "password": "wrong-password"
						}
						"""))
				.andExpect(status().isUnauthorized())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode json = objectMapper.readTree(responseBody);
		assertThat(json.path("timestamp").asText()).isNotBlank();
		assertThat(json.path("status").asInt()).isEqualTo(401);
		assertThat(json.path("error").asText()).isEqualTo("Unauthorized");
		assertThat(json.path("message").asText()).isNotBlank();
		assertThat(json.path("path").asText()).isEqualTo("/api/auth/login");
		assertThat(json.path("requestId").asText()).isNotBlank();
		assertThat(json.path("validationErrors").isMissingNode()).isTrue();
		assertThat(json.path("debugDetails").isMissingNode()).isTrue();
	}

	@Test
	void validationErrorsAreReturnedAsStructuredDetails() throws Exception {
		ensureUser("user-a", UserRole.USER);

		String responseBody = mockMvc.perform(post("/api/settings")
					.with(user("user-a").roles("USER"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "professorName": "Dr. Stone",
						  "citationAnalysis": true
						}
						"""))
				.andExpect(status().isBadRequest())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode json = objectMapper.readTree(responseBody);
		assertThat(json.path("status").asInt()).isEqualTo(400);
		assertThat(json.path("message").asText()).isEqualTo("Request validation failed.");
		assertThat(json.path("requestId").asText()).isNotBlank();
		assertThat(json.path("validationErrors").isArray()).isTrue();
		assertThat(json.path("validationErrors")).hasSizeGreaterThanOrEqualTo(3);
		assertThat(json.path("validationErrors").toString()).contains("colorTheme");
		assertThat(json.path("validationErrors").toString()).contains("displayDensity");
		assertThat(json.path("validationErrors").toString()).contains("readingLayout");
	}

	@Test
	void forbiddenAccessReturnsStructuredError() throws Exception {
		String responseBody = mockMvc.perform(get("/api/users")
					.with(user("user-only").roles("USER")))
				.andExpect(status().isForbidden())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode json = objectMapper.readTree(responseBody);
		assertThat(json.path("status").asInt()).isEqualTo(403);
		assertThat(json.path("error").asText()).isEqualTo("Forbidden");
		assertThat(json.path("message").asText()).isEqualTo("You do not have permission to access this resource.");
		assertThat(json.path("path").asText()).isEqualTo("/api/users");
		assertThat(json.path("requestId").asText()).isNotBlank();
	}

	@Test
	void malformedJsonReturnsStructuredBadRequestError() throws Exception {
		ensureUser("user-a", UserRole.USER);

		String responseBody = mockMvc.perform(post("/api/settings")
					.with(user("user-a").roles("USER"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{"))
				.andExpect(status().isBadRequest())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode json = objectMapper.readTree(responseBody);
		assertThat(json.path("status").asInt()).isEqualTo(400);
		assertThat(json.path("message").asText()).startsWith("Malformed JSON request:");
		assertThat(json.path("path").asText()).isEqualTo("/api/settings");
		assertThat(json.path("requestId").asText()).isNotBlank();
		assertThat(json.path("validationErrors").isMissingNode()).isTrue();
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
}