package com.academic.integrity.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.academic.integrity.review.domain.Document;
import com.academic.integrity.review.domain.ReviewPriority;
import com.academic.integrity.review.domain.ReviewStatus;
import com.academic.integrity.review.repository.DocumentRepository;
import com.academic.integrity.review.repository.ReviewNoteRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewNoteControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DocumentRepository documentRepository;

	@Autowired
	private ReviewNoteRepository reviewNoteRepository;

	@AfterEach
	void tearDown() {
		reviewNoteRepository.deleteAll();
		documentRepository.deleteAll();
	}

	@Test
	void savingReviewNoteWithIncompleteFinalReviewSetsDocumentStatusToInReview() throws Exception {
		Document document = createDocument();

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
		Document document = createDocument();

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

	private Document createDocument() {
		Document document = new Document();
		document.setTitle("Review Target");
		document.setStudentName("Jordan Student");
		document.setCourse("ENG-301");
		document.setAcademicYear("2025-2026");
		document.setSubmissionDate(LocalDate.now());
		document.setReviewPriority(ReviewPriority.MEDIUM);
		document.setReviewStatus(ReviewStatus.PENDING);
		return documentRepository.saveAndFlush(document);
	}
}