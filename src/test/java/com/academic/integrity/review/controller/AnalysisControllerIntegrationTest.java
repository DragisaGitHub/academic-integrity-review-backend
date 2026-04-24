package com.academic.integrity.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.AnalysisStatus;
import com.academic.integrity.review.domain.ApplicationSettings;
import com.academic.integrity.review.domain.Document;
import com.academic.integrity.review.domain.ReviewPriority;
import com.academic.integrity.review.domain.ReviewStatus;
import com.academic.integrity.review.domain.User;
import com.academic.integrity.review.domain.UserRole;
import com.academic.integrity.review.repository.ApplicationSettingsRepository;
import com.academic.integrity.review.repository.AnalysisRepository;
import com.academic.integrity.review.repository.DocumentRepository;
import com.academic.integrity.review.repository.FindingRepository;
import com.academic.integrity.review.repository.NotificationRepository;
import com.academic.integrity.review.repository.TextSegmentRepository;
import com.academic.integrity.review.repository.UserRepository;
import com.academic.integrity.review.service.LlmClientService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private DocumentRepository documentRepository;

	@Autowired
	private AnalysisRepository analysisRepository;

	@Autowired
	private FindingRepository findingRepository;

	@Autowired
	private ApplicationSettingsRepository applicationSettingsRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private TextSegmentRepository textSegmentRepository;

	@Autowired
	private UserRepository userRepository;

	@MockBean
	private LlmClientService llmClientService;

	@TempDir
	Path tempDir;

	@AfterEach
	void tearDown() {
		notificationRepository.deleteAll();
		findingRepository.deleteAll();
		textSegmentRepository.deleteAll();
		analysisRepository.deleteAll();
		applicationSettingsRepository.deleteAll();
		documentRepository.deleteAll();
	}

	@Test
	void getAllAnalysesReturnsPersistedAnalyses() throws Exception {
		User admin = ensureUser("admin", UserRole.ADMIN);
		Document document = createDocument(admin, "Listing analyses content.");
		Analysis analysis = new Analysis();
		analysis.setUser(admin);
		analysis.setDocument(document);
		analysis.setAnalysisDate(LocalDate.now());
		analysis.setAnalysisStatus(AnalysisStatus.COMPLETED);
		analysis.setFullText("Listing analyses content.");
		analysis = analysisRepository.saveAndFlush(analysis);

		String response = mockMvc.perform(get("/api/analyses").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode json = objectMapper.readTree(response);
		assertThat(json).hasSize(1);
		assertThat(json.get(0).path("id").asLong()).isEqualTo(analysis.getId());
		assertThat(json.get(0).path("document").path("id").asLong()).isEqualTo(document.getId());
		assertThat(json.get(0).has("fullText")).isFalse();
	}

	@Test
	void getAnalysisByDocumentIdDoesNotExposeFullText() throws Exception {
		User admin = ensureUser("admin", UserRole.ADMIN);
		Document document = createDocument(admin, "Document detail exposure check.");
		Analysis analysis = new Analysis();
		analysis.setUser(admin);
		analysis.setDocument(document);
		analysis.setAnalysisDate(LocalDate.now());
		analysis.setAnalysisStatus(AnalysisStatus.COMPLETED);
		analysis.setFullText("Document detail exposure check.");
		analysisRepository.saveAndFlush(analysis);

		String response = mockMvc.perform(get("/api/analyses/document/{documentId}", document.getId())
					.with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode json = objectMapper.readTree(response);
		assertThat(json.has("fullText")).isFalse();
	}

	@Test
	void getAnalysisFullTextReturnsOnDemandForOwner() throws Exception {
		User admin = ensureUser("admin", UserRole.ADMIN);
		Document document = createDocument(admin, "Bridge endpoint content.");
		Analysis analysis = new Analysis();
		analysis.setUser(admin);
		analysis.setDocument(document);
		analysis.setAnalysisDate(LocalDate.now());
		analysis.setAnalysisStatus(AnalysisStatus.COMPLETED);
		analysis.setFullText("Bridge endpoint content.");
		analysis = analysisRepository.saveAndFlush(analysis);

		String response = mockMvc.perform(get("/api/analyses/{analysisId}/text", analysis.getId())
					.with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode json = objectMapper.readTree(response);
		assertThat(json.path("analysisId").asLong()).isEqualTo(analysis.getId());
		assertThat(json.path("fullText").asText()).isEqualTo("Bridge endpoint content.");
	}

	@Test
	void getAnalysisTextSegmentsBackfillsSegmentsAndSupportsRangeRetrieval() throws Exception {
		User admin = ensureUser("admin", UserRole.ADMIN);
		Document document = createDocument(admin, "unused stored text");
		Analysis analysis = new Analysis();
		analysis.setUser(admin);
		analysis.setDocument(document);
		analysis.setAnalysisDate(LocalDate.now());
		analysis.setAnalysisStatus(AnalysisStatus.COMPLETED);
		analysis.setFullText(
				"First paragraph is intentionally long enough to remain a standalone segment for retrieval testing."
						+ "\n\nTiny."
						+ "\n\nThird paragraph is also long enough to remain visible when requesting a later segment range.");
		analysis = analysisRepository.saveAndFlush(analysis);

		String response = mockMvc.perform(get("/api/analyses/{analysisId}/text/segments", analysis.getId())
					.with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode json = objectMapper.readTree(response);
		assertThat(json).hasSize(2);
		assertThat(json.get(0).path("segmentIndex").asInt()).isEqualTo(0);
		assertThat(json.get(0).path("content").asText()).contains("Tiny.");
		assertThat(json.get(1).path("segmentIndex").asInt()).isEqualTo(1);
		assertThat(textSegmentRepository.countByAnalysis_Id(analysis.getId())).isEqualTo(2);

		String rangeResponse = mockMvc.perform(get("/api/analyses/{analysisId}/text/segments", analysis.getId())
					.with(user("admin").roles("ADMIN"))
					.param("from", "1")
					.param("to", "1"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode rangeJson = objectMapper.readTree(rangeResponse);
		assertThat(rangeJson).hasSize(1);
		assertThat(rangeJson.get(0).path("segmentIndex").asInt()).isEqualTo(1);
		assertThat(rangeJson.get(0).path("content").asText()).contains("Third paragraph");
	}

	@Test
	void textEndpointsAreScopedToOwningUser() throws Exception {
		User userA = ensureUser("user-a", UserRole.USER);
		ensureUser("user-b", UserRole.USER);
		Document document = createDocument(userA, "Private text content.");
		Analysis analysis = new Analysis();
		analysis.setUser(userA);
		analysis.setDocument(document);
		analysis.setAnalysisDate(LocalDate.now());
		analysis.setAnalysisStatus(AnalysisStatus.COMPLETED);
		analysis.setFullText("Private text content.\n\nSecond paragraph.");
		analysis = analysisRepository.saveAndFlush(analysis);

		mockMvc.perform(get("/api/analyses/{analysisId}/text", analysis.getId())
					.with(user("user-b").roles("USER")))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/analyses/{analysisId}/text/segments", analysis.getId())
					.with(user("user-b").roles("USER")))
				.andExpect(status().isNotFound());
	}

	@Test
	void analysisNotesCanBeSavedAndLoaded() throws Exception {
		User admin = ensureUser("admin", UserRole.ADMIN);
		Document document = createDocument(admin, "Notes content.");
		Analysis analysis = new Analysis();
		analysis.setUser(admin);
		analysis.setDocument(document);
		analysis.setAnalysisDate(LocalDate.now());
		analysis.setAnalysisStatus(AnalysisStatus.COMPLETED);
		analysis = analysisRepository.saveAndFlush(analysis);

		String saveResponse = mockMvc.perform(post("/api/analyses/{analysisId}/notes", analysis.getId())
						.with(user("admin").roles("ADMIN"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
							{
							  "notes": "Professor follow-up notes for this analysis."
							}
							"""))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode savedJson = objectMapper.readTree(saveResponse);
		assertThat(savedJson.path("notes").asText()).isEqualTo("Professor follow-up notes for this analysis.");

		String getResponse = mockMvc.perform(get("/api/analyses/{analysisId}/notes", analysis.getId())
						.with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode getJson = objectMapper.readTree(getResponse);
		assertThat(getJson.path("analysisId").asLong()).isEqualTo(analysis.getId());
		assertThat(getJson.path("notes").asText()).isEqualTo("Professor follow-up notes for this analysis.");
	}

	@Test
	void getAnalysisNotesReturnsNotFoundWhenMissing() throws Exception {
		User admin = ensureUser("admin", UserRole.ADMIN);
		Document document = createDocument(admin, "No analysis notes yet.");
		Analysis analysis = new Analysis();
		analysis.setUser(admin);
		analysis.setDocument(document);
		analysis.setAnalysisDate(LocalDate.now());
		analysis.setAnalysisStatus(AnalysisStatus.COMPLETED);
		analysisRepository.saveAndFlush(analysis);

		mockMvc.perform(get("/api/analyses/{analysisId}/notes", analysis.getId())
				.with(user("admin").roles("ADMIN")))
				.andExpect(status().isNotFound());
	}

	@Test
	void createAnalysisRunsAsyncAndPersistsFindings() throws Exception {
		User admin = ensureUser("admin", UserRole.ADMIN);
		Document document = createDocument(admin, "This seminar paper makes unsupported claims without sources.");
		when(llmClientService.analyze(anyString())).thenReturn(new LlmClientService.LlmAnalysisResult(
				"{" +
					"\"findings\":[{" +
					"\"category\":\"CITATION_ISSUE\"," +
					"\"severity\":\"HIGH\"," +
					"\"title\":\"Missing supporting citation\"," +
					"\"explanation\":\"A factual claim is not supported by any citation.\"," +
					"\"excerpt\":\"Important factual statement\"," +
					"\"paragraphLocation\":\"Paragraph 1\"," +
					"\"suggestedAction\":\"Provide a credible source.\"}]}",
				"test-model",
				321
		));

		MvcResult result = mockMvc.perform(post("/api/analyses")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{" + "\"documentId\":" + document.getId() + "}"))
				.andExpect(status().isAccepted())
				.andReturn();

		JsonNode createJson = objectMapper.readTree(result.getResponse().getContentAsString());
		assertThat(createJson.has("fullText")).isFalse();

		Long analysisId = responseAnalysisId(result);
		awaitStatus(analysisId, AnalysisStatus.COMPLETED);
		awaitNotificationCount(1);

		Analysis analysis = analysisRepository.findById(analysisId).orElseThrow();
		assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
		assertThat(analysis.getModelName()).isEqualTo("test-model");
		assertThat(analysis.getTotalTokensUsed()).isEqualTo(321);
		assertThat(analysis.getFullText()).contains("unsupported claims");
		assertThat(findingRepository.findAll()).hasSize(1);

		String documentBody = mockMvc.perform(get("/api/documents/{id}", document.getId())
				.with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode documentJson = objectMapper.readTree(documentBody);
		assertThat(documentJson.path("hasAnalysis").asBoolean()).isTrue();
		assertThat(documentJson.path("analysisId").asLong()).isEqualTo(analysisId);
		assertThat(documentJson.path("analysisStatus").asText()).isEqualTo("COMPLETED");
		assertThat(documentJson.path("analysisErrorMessage").isNull()).isTrue();

		mockMvc.perform(get("/api/analyses/{analysisId}/status", analysisId)
				.with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());

		String segmentsBody = mockMvc.perform(get("/api/analyses/{analysisId}/text/segments", analysisId)
					.with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(objectMapper.readTree(segmentsBody)).isNotEmpty();
	}

	@Test
	void createAnalysisUsesSettingsOfTriggeringUserInPrompt() throws Exception {
		User userA = ensureUser("user-a", UserRole.USER);
		User userB = ensureUser("user-b", UserRole.USER);

		ApplicationSettings settings = new ApplicationSettings();
		settings.setUser(userA);
		settings.setEmail("");
		settings.setCitationAnalysis(true);
		settings.setReferenceValidation(false);
		settings.setFactualConsistencyReview(true);
		settings.setWritingStyleConsistency(false);
		settings.setAiReviewAssistance(true);
		settings.setLocalAiEnabled(false);
		settings.setDocumentRetentionDays(30);
		settings.setAutoDeleteReviewedDocuments(false);
		settings.setStorageLocation("");
		applicationSettingsRepository.saveAndFlush(settings);

		ApplicationSettings otherUserSettings = new ApplicationSettings();
		otherUserSettings.setUser(userB);
		otherUserSettings.setEmail("");
		otherUserSettings.setCitationAnalysis(false);
		otherUserSettings.setReferenceValidation(true);
		otherUserSettings.setFactualConsistencyReview(false);
		otherUserSettings.setWritingStyleConsistency(true);
		otherUserSettings.setAiReviewAssistance(false);
		otherUserSettings.setLocalAiEnabled(false);
		otherUserSettings.setDocumentRetentionDays(30);
		otherUserSettings.setAutoDeleteReviewedDocuments(false);
		otherUserSettings.setStorageLocation("");
		applicationSettingsRepository.saveAndFlush(otherUserSettings);

		Document document = createDocument(userA, "Prompt settings document content.");
		when(llmClientService.analyze(anyString())).thenReturn(new LlmClientService.LlmAnalysisResult(
				"{\"findings\":[]}",
				"test-model",
				42
		));

		MvcResult result = mockMvc.perform(post("/api/analyses")
				.with(user("user-a").roles("USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{" + "\"documentId\":" + document.getId() + "}"))
				.andExpect(status().isAccepted())
				.andReturn();

		Long analysisId = responseAnalysisId(result);
		awaitStatus(analysisId, AnalysisStatus.COMPLETED);
		awaitNotificationCount(1);

		ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
		verify(llmClientService).analyze(promptCaptor.capture());
		String prompt = promptCaptor.getValue();
		assertThat(prompt).contains("Enabled review modules:");
		assertThat(prompt).contains("Citation analysis is enabled");
		assertThat(prompt).contains("Factual consistency review is enabled");
		assertThat(prompt).contains("AI review assistance is enabled");
		assertThat(prompt).contains("Return at most 8 findings.");
		assertThat(prompt).contains("Keep excerpt at or below 220 characters.");
		assertThat(prompt).doesNotContain("Reference validation is enabled");
		assertThat(prompt).doesNotContain("Writing style consistency is enabled");
	}

	@Test
	void createAnalysisFailsWithSafeMessageWhenAiReturnsMalformedFindingsJson() throws Exception {
		User admin = ensureUser("admin", UserRole.ADMIN);
		Document document = createDocument(admin, "A longer paper body that triggers malformed output handling.");
		when(llmClientService.analyze(anyString())).thenReturn(new LlmClientService.LlmAnalysisResult(
				"{" +
						"\"findings\":[{" +
						"\"category\":\"CITATION_ISSUE\"," +
						"\"severity\":\"HIGH\"," +
						"\"title\":\"Truncated finding\"," +
						"\"explanation\":\"The payload breaks before it closes",
				"test-model",
				450
		));

		MvcResult result = mockMvc.perform(post("/api/analyses")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{" + "\"documentId\":" + document.getId() + "}"))
				.andExpect(status().isAccepted())
				.andReturn();

		Long analysisId = responseAnalysisId(result);
		awaitStatus(analysisId, AnalysisStatus.FAILED);
		awaitNotificationCount(1);

		Analysis analysis = analysisRepository.findById(analysisId).orElseThrow();
		assertThat(analysis.getErrorMessage())
				.isEqualTo("AI returned malformed findings output. Please retry the analysis.");
		assertThat(notificationRepository.findAll()).singleElement().satisfies(notification ->
				assertThat(notification.getMessage())
						.isEqualTo("Seminar Paper — AI returned malformed findings output. Please retry the analysis."));
	}

	@Test
	void retryAnalysisRequeuesFailedAnalysis() throws Exception {
		User admin = ensureUser("admin", UserRole.ADMIN);
		Document document = createDocument(admin, "A short document for retry testing.");
		doThrow(new IllegalStateException("Simulated OpenAI failure"))
				.when(llmClientService).analyze(anyString());

		MvcResult initial = mockMvc.perform(post("/api/analyses")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{" + "\"documentId\":" + document.getId() + "}"))
				.andExpect(status().isAccepted())
				.andReturn();

		Long analysisId = responseAnalysisId(initial);
		awaitStatus(analysisId, AnalysisStatus.FAILED);
		awaitNotificationCount(1);

		Analysis failed = analysisRepository.findById(analysisId).orElseThrow();
		assertThat(failed.getErrorMessage()).contains("Simulated OpenAI failure");

		reset(llmClientService);
		when(llmClientService.analyze(anyString())).thenReturn(new LlmClientService.LlmAnalysisResult(
				"{\"findings\":[]}",
				"retry-model",
				111
		));

		mockMvc.perform(post("/api/analyses/{analysisId}/retry", analysisId)
				.with(user("admin").roles("ADMIN")))
				.andExpect(status().isAccepted());

		awaitStatus(analysisId, AnalysisStatus.COMPLETED);
		awaitNotificationCount(2);
		Analysis completed = analysisRepository.findById(analysisId).orElseThrow();
		assertThat(completed.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
		assertThat(completed.getErrorMessage()).isNull();
	}

	@Test
	void analysesAreScopedToOwningUser() throws Exception {
		User userA = ensureUser("user-a", UserRole.USER);
		ensureUser("user-b", UserRole.USER);
		Document document = createDocument(userA, "Owned analysis content.");

		when(llmClientService.analyze(anyString())).thenReturn(new LlmClientService.LlmAnalysisResult(
				"{\"findings\":[]}",
				"test-model",
				12
		));

		MvcResult result = mockMvc.perform(post("/api/analyses")
				.with(user("user-a").roles("USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{" + "\"documentId\":" + document.getId() + "}"))
				.andExpect(status().isAccepted())
				.andReturn();

		Long analysisId = responseAnalysisId(result);
		awaitStatus(analysisId, AnalysisStatus.COMPLETED);
		awaitNotificationCount(1);

		String userAList = mockMvc.perform(get("/api/analyses").with(user("user-a").roles("USER")))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(objectMapper.readTree(userAList)).hasSize(1);

		String userBList = mockMvc.perform(get("/api/analyses").with(user("user-b").roles("USER")))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(objectMapper.readTree(userBList)).isEmpty();

		mockMvc.perform(get("/api/analyses/{analysisId}/status", analysisId).with(user("user-b").roles("USER")))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/analyses/{analysisId}/text", analysisId).with(user("user-b").roles("USER")))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/analyses/{analysisId}/text/segments", analysisId).with(user("user-b").roles("USER")))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/analyses/{analysisId}/retry", analysisId).with(user("user-b").roles("USER")))
				.andExpect(status().isNotFound());
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

	private Document createDocument(String content) throws Exception {
		return createDocument(ensureUser("admin", UserRole.ADMIN), content);
	}

	private Document createDocument(User owner, String content) throws Exception {
		Path file = tempDir.resolve("paper.txt");
		Files.writeString(file, content);

		Document document = new Document();
		document.setUser(owner);
		document.setTitle("Seminar Paper");
		document.setStudentName("Jane Student");
		document.setCourse("Academic Writing");
		document.setSubmissionDate(LocalDate.now());
		document.setOriginalFilename("paper.txt");
		document.setStoredFilename(file.getFileName().toString());
		document.setStoredPath(file.toString());
		document.setContentType("text/plain");
		document.setFileSize((long) content.length());
		document.setReviewPriority(ReviewPriority.MEDIUM);
		document.setReviewStatus(ReviewStatus.PENDING);
		return documentRepository.saveAndFlush(document);
	}

	private Long responseAnalysisId(MvcResult result) throws Exception {
		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
		return body.path("id").asLong();
	}

	private void awaitStatus(Long analysisId, AnalysisStatus expected) throws Exception {
		long timeoutAt = System.currentTimeMillis() + 5000;
		while (System.currentTimeMillis() < timeoutAt) {
			AnalysisStatus current = analysisRepository.findById(analysisId)
					.map(Analysis::getAnalysisStatus)
					.orElseThrow();
			if (current == expected) {
				return;
			}
			Thread.sleep(100);
		}
		throw new AssertionError("Timed out waiting for analysis status " + expected);
	}

	private void awaitNotificationCount(int expected) throws Exception {
		long timeoutAt = System.currentTimeMillis() + 10000;
		while (System.currentTimeMillis() < timeoutAt) {
			if (notificationRepository.count() == expected) {
				return;
			}
			Thread.sleep(100);
		}
		throw new AssertionError("Timed out waiting for notification count " + expected
				+ ", actual count=" + notificationRepository.count());
	}
}