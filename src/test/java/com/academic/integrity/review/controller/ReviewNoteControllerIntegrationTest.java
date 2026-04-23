package com.academic.integrity.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.academic.integrity.review.domain.Document;
import com.academic.integrity.review.domain.ReviewPriority;
import com.academic.integrity.review.domain.ReviewStatus;
import com.academic.integrity.review.domain.User;
import com.academic.integrity.review.domain.UserRole;
import com.academic.integrity.review.repository.DocumentRepository;
import com.academic.integrity.review.repository.ReviewNoteRepository;
import com.academic.integrity.review.repository.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class ReviewNoteControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DocumentRepository documentRepository;

	@Autowired
	private ReviewNoteRepository reviewNoteRepository;

	@Autowired
	private UserRepository userRepository;

	@AfterEach
	void tearDown() {
		reviewNoteRepository.deleteAll();
		documentRepository.deleteAll();
	}

	@Test
	void savingReviewNoteWithIncompleteFinalReviewSetsDocumentStatusToInReview() throws Exception {
		Document document = createDocument(ensureUser("admin", UserRole.ADMIN));

		mockMvc.perform(post("/api/documents/{documentId}/review-note", document.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
							{
							  "notes": "Initial review notes",
							  "referencesChecked": true,
							  "oralDefenseRequired": false,
							  "factualIssuesDiscussed": false,
							  "finalReviewCompleted": false,
							  "finalDecision": "ACCEPT"
							}
							"""))
				.andExpect(status().isOk());

		Document updated = documentRepository.findById(document.getId()).orElseThrow();
		assertThat(updated.getReviewStatus()).isEqualTo(ReviewStatus.IN_REVIEW);
	}

	@Test
	void savingReviewNoteWithCompletedFinalReviewSetsDocumentStatusToReviewed() throws Exception {
		Document document = createDocument(ensureUser("admin", UserRole.ADMIN));

		mockMvc.perform(post("/api/documents/{documentId}/review-note", document.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
							{
							  "notes": "Final review notes",
							  "referencesChecked": true,
							  "oralDefenseRequired": false,
							  "factualIssuesDiscussed": true,
							  "finalReviewCompleted": true,
							  "finalDecision": "ACCEPT_WITH_REVISIONS"
							}
							"""))
				.andExpect(status().isOk());

		Document updated = documentRepository.findById(document.getId()).orElseThrow();
		assertThat(updated.getReviewStatus()).isEqualTo(ReviewStatus.REVIEWED);
	}

	@Test
	void reviewNotesAreScopedToOwningUser() throws Exception {
		User userA = ensureUser("user-a", UserRole.USER);
		ensureUser("user-b", UserRole.USER);
		Document document = createDocument(userA);

		mockMvc.perform(post("/api/documents/{documentId}/review-note", document.getId())
						.with(user("user-a").roles("USER"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
							{
							  "notes": "Private review notes",
							  "referencesChecked": true,
							  "oralDefenseRequired": false,
							  "factualIssuesDiscussed": false,
							  "finalReviewCompleted": false,
							  "finalDecision": "ACCEPT"
							}
							"""))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/documents/{documentId}/review-note", document.getId())
						.with(user("user-b").roles("USER")))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/documents/{documentId}/review-note", document.getId())
						.with(user("user-b").roles("USER"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
							{
							  "notes": "Unauthorized",
							  "referencesChecked": true,
							  "oralDefenseRequired": false,
							  "factualIssuesDiscussed": false,
							  "finalReviewCompleted": false,
							  "finalDecision": "ACCEPT"
							}
							"""))
				.andExpect(status().isNotFound());
	}

	private Document createDocument(User owner) {
		Document document = new Document();
		document.setUser(owner);
		document.setTitle("Review Target");
		document.setStudentName("Jordan Student");
		document.setCourse("ENG-301");
		document.setAcademicYear("2025-2026");
		document.setSubmissionDate(LocalDate.now());
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
}