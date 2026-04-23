package com.academic.integrity.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.AnalysisStatus;
import com.academic.integrity.review.domain.Document;
import com.academic.integrity.review.domain.Notification;
import com.academic.integrity.review.domain.ReviewPriority;
import com.academic.integrity.review.domain.ReviewStatus;
import com.academic.integrity.review.domain.User;
import com.academic.integrity.review.domain.UserRole;
import com.academic.integrity.review.repository.AnalysisRepository;
import com.academic.integrity.review.repository.DocumentRepository;
import com.academic.integrity.review.repository.NotificationRepository;
import com.academic.integrity.review.repository.UserRepository;
import com.academic.integrity.review.service.LlmClientService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class NotificationControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private DocumentRepository documentRepository;

	@Autowired
	private AnalysisRepository analysisRepository;

	@Autowired
	private UserRepository userRepository;

	@MockBean
	private LlmClientService llmClientService;

	@TempDir
	Path tempDir;

	@AfterEach
	void tearDown() {
		notificationRepository.deleteAll();
		analysisRepository.deleteAll();
		documentRepository.deleteAll();
	}

	@Test
	void listingNotificationsReturnsLatestNotificationsAndUnreadCount() throws Exception {
		notificationRepository.save(createNotification("first", false, Instant.now().minusSeconds(60)));
		notificationRepository.save(createNotification("second", true, Instant.now().minusSeconds(30)));
		notificationRepository.save(createNotification("third", false, Instant.now()));

		String response = mockMvc.perform(get("/api/notifications").param("limit", "2"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode json = objectMapper.readTree(response);
		assertThat(json.path("unreadCount").asLong()).isEqualTo(2);
		assertThat(json.path("notifications")).hasSize(2);
		assertThat(json.path("notifications").get(0).path("id").asText()).isEqualTo("third");
		assertThat(json.path("notifications").get(1).path("id").asText()).isEqualTo("second");
	}

	@Test
	void markOneReadUpdatesUnreadCount() throws Exception {
		Notification notification = notificationRepository.save(createNotification("one", false, Instant.now()));

		mockMvc.perform(patch("/api/notifications/{id}/read", notification.getId()))
				.andExpect(status().isNoContent());

		Notification updated = notificationRepository.findById(notification.getId()).orElseThrow();
		assertThat(updated.isRead()).isTrue();
		assertThat(notificationRepository.countByReadFalse()).isZero();
	}

	@Test
	void markAllReadMarksAllUnreadNotifications() throws Exception {
		notificationRepository.save(createNotification("one", false, Instant.now().minusSeconds(10)));
		notificationRepository.save(createNotification("two", false, Instant.now()));

		mockMvc.perform(patch("/api/notifications/read-all"))
				.andExpect(status().isNoContent());

		assertThat(notificationRepository.countByReadFalse()).isZero();
		assertThat(notificationRepository.findAll()).allMatch(Notification::isRead);
	}

	@Test
	void analysisCompletedCreatesNotification() throws Exception {
		Document document = createDocument("Completed analysis content.", "Completed Paper");
		when(llmClientService.analyze(anyString())).thenReturn(new LlmClientService.LlmAnalysisResult(
				"{\"findings\":[]}",
				"test-model",
				100
		));

		Long analysisId = createAnalysis(document.getId());
		awaitStatus(analysisId, AnalysisStatus.COMPLETED);
		awaitNotificationCount(1);

		assertThat(notificationRepository.findAll()).hasSize(1);
		Notification notification = notificationRepository.findAll().getFirst();
		assertThat(notification.getType()).isEqualTo("analysis-completed");
		assertThat(notification.getTitle()).isEqualTo("Analysis complete");
		assertThat(notification.getMessage()).isEqualTo("Completed Paper — analysis finished successfully");
		assertThat(notification.getSeverity()).isEqualTo("success");
		assertThat(notification.getDocumentId()).isEqualTo(document.getId());
		assertThat(notification.getAnalysisId()).isEqualTo(analysisId);
		assertThat(notification.getRoute()).isEqualTo("/analysis/" + document.getId());
		assertThat(notification.isRead()).isFalse();
	}

	@Test
	void analysisFailedCreatesNotification() throws Exception {
		Document document = createDocument("Failed analysis content.", "Failed Paper");
		doThrow(new IllegalStateException("Simulated OpenAI failure"))
				.when(llmClientService).analyze(anyString());

		Long analysisId = createAnalysis(document.getId());
		awaitStatus(analysisId, AnalysisStatus.FAILED);
		awaitNotificationCount(1);

		assertThat(notificationRepository.findAll()).hasSize(1);
		Notification notification = notificationRepository.findAll().getFirst();
		assertThat(notification.getType()).isEqualTo("analysis-failed");
		assertThat(notification.getTitle()).isEqualTo("Analysis failed");
		assertThat(notification.getMessage()).isEqualTo("Failed Paper — Simulated OpenAI failure");
		assertThat(notification.getSeverity()).isEqualTo("error");
		assertThat(notification.getDocumentId()).isEqualTo(document.getId());
		assertThat(notification.getAnalysisId()).isEqualTo(analysisId);
		assertThat(notification.getRoute()).isEqualTo("/analysis/" + document.getId());
	}

	private Notification createNotification(String id, boolean read, Instant createdAt) {
		Notification notification = new Notification();
		notification.setId(id);
		notification.setType("analysis-completed");
		notification.setTitle("Analysis complete");
		notification.setMessage("Message for " + id);
		notification.setSeverity("success");
		notification.setRead(read);
		notification.setCreatedAt(createdAt);
		notification.setRoute("/analysis/1");
		return notification;
	}

	private Document createDocument(String content, String title) throws Exception {
		User admin = ensureUser("admin", UserRole.ADMIN);
		Path file = tempDir.resolve(title.replace(' ', '-') + ".txt");
		Files.writeString(file, content);

		Document document = new Document();
		document.setUser(admin);
		document.setTitle(title);
		document.setStudentName("Taylor Student");
		document.setCourse("ENG-410");
		document.setAcademicYear("2025-2026");
		document.setSubmissionDate(LocalDate.now());
		document.setOriginalFilename(file.getFileName().toString());
		document.setStoredFilename(file.getFileName().toString());
		document.setStoredPath(file.toString());
		document.setContentType(MediaType.TEXT_PLAIN_VALUE);
		document.setFileSize((long) content.length());
		document.setReviewPriority(ReviewPriority.MEDIUM);
		document.setReviewStatus(ReviewStatus.PENDING);
		return documentRepository.saveAndFlush(document);
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

	private Long createAnalysis(Long documentId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/analyses")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{" + "\"documentId\":" + documentId + "}"))
				.andExpect(status().isAccepted())
				.andReturn();
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
		long timeoutAt = System.currentTimeMillis() + 5000;
		while (System.currentTimeMillis() < timeoutAt) {
			if (notificationRepository.count() == expected) {
				return;
			}
			Thread.sleep(100);
		}
		throw new AssertionError("Timed out waiting for notification count " + expected);
	}
}