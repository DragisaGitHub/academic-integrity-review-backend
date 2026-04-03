package com.academic.integrity.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.AnalysisStatus;
import com.academic.integrity.review.domain.Document;
import com.academic.integrity.review.domain.Finding;
import com.academic.integrity.review.domain.FindingCategory;
import com.academic.integrity.review.domain.FindingSeverity;
import com.academic.integrity.review.domain.ReviewNote;
import com.academic.integrity.review.domain.ReviewPriority;
import com.academic.integrity.review.domain.ReviewStatus;
import com.academic.integrity.review.repository.AnalysisRepository;
import com.academic.integrity.review.repository.DocumentRepository;
import com.academic.integrity.review.repository.FindingRepository;
import com.academic.integrity.review.repository.ReviewNoteRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DocumentRepository documentRepository;

	@Autowired
	private AnalysisRepository analysisRepository;

	@Autowired
	private FindingRepository findingRepository;

	@Autowired
	private ReviewNoteRepository reviewNoteRepository;

	@TempDir
	Path tempDir;

	@AfterEach
	void tearDown() {
		findingRepository.deleteAll();
		analysisRepository.deleteAll();
		reviewNoteRepository.deleteAll();
		documentRepository.deleteAll();
	}

	@Test
	void deleteDocumentRemovesAggregateAndStoredFile() throws Exception {
		Path file = tempDir.resolve("history-paper.txt");
		Files.writeString(file, "Stored paper content");

		Document document = new Document();
		document.setTitle("History Entry");
		document.setStudentName("Taylor Student");
		document.setCourse("ENG-201");
		document.setSubmissionDate(LocalDate.now());
		document.setOriginalFilename("history-paper.txt");
		document.setStoredFilename(file.getFileName().toString());
		document.setStoredPath(file.toString());
		document.setContentType("text/plain");
		document.setFileSize(Files.size(file));
		document.setReviewPriority(ReviewPriority.MEDIUM);
		document.setReviewStatus(ReviewStatus.PENDING);
		document = documentRepository.saveAndFlush(document);

		Analysis analysis = new Analysis();
		analysis.setDocument(document);
		analysis.setAnalysisDate(LocalDate.now());
		analysis.setAnalysisStatus(AnalysisStatus.COMPLETED);
		analysis.setFullText("Extracted text");
		analysis = analysisRepository.saveAndFlush(analysis);

		Finding finding = new Finding();
		finding.setAnalysis(analysis);
		finding.setCategory(FindingCategory.CITATION_ISSUE);
		finding.setSeverity(FindingSeverity.MEDIUM);
		finding.setTitle("Example finding");
		findingRepository.saveAndFlush(finding);

		ReviewNote reviewNote = new ReviewNote();
		reviewNote.setDocument(document);
		reviewNote.setNotes("Reviewed");
		reviewNoteRepository.saveAndFlush(reviewNote);

		mockMvc.perform(delete("/api/documents/{id}", document.getId()))
				.andExpect(status().isNoContent());

		assertThat(documentRepository.findById(document.getId())).isEmpty();
		assertThat(analysisRepository.findById(analysis.getId())).isEmpty();
		assertThat(findingRepository.findAllByAnalysis_Id(analysis.getId())).isEmpty();
		assertThat(reviewNoteRepository.findByDocument_Id(document.getId())).isEmpty();
		assertThat(Files.exists(file)).isFalse();
	}

	@Test
	void deleteDocumentReturnsConflictWhenAnalysisIsRunning() throws Exception {
		Path file = tempDir.resolve("in-progress-paper.txt");
		Files.writeString(file, "Stored paper content");

		Document document = new Document();
		document.setTitle("Running Analysis Entry");
		document.setStudentName("Alex Student");
		document.setCourse("ENG-202");
		document.setSubmissionDate(LocalDate.now());
		document.setOriginalFilename("in-progress-paper.txt");
		document.setStoredFilename(file.getFileName().toString());
		document.setStoredPath(file.toString());
		document.setContentType("text/plain");
		document.setFileSize(Files.size(file));
		document.setReviewPriority(ReviewPriority.MEDIUM);
		document.setReviewStatus(ReviewStatus.PENDING);
		document = documentRepository.saveAndFlush(document);

		Analysis analysis = new Analysis();
		analysis.setDocument(document);
		analysis.setAnalysisDate(LocalDate.now());
		analysis.setAnalysisStatus(AnalysisStatus.ANALYZING);
		analysisRepository.saveAndFlush(analysis);

		mockMvc.perform(delete("/api/documents/{id}", document.getId()))
				.andExpect(status().isConflict());

		assertThat(documentRepository.findById(document.getId())).isPresent();
		assertThat(Files.exists(file)).isTrue();
	}

	@Test
	void deleteDocumentReturnsNotFoundWhenMissing() throws Exception {
		mockMvc.perform(delete("/api/documents/{id}", 999999L))
				.andExpect(status().isNotFound());
	}
}