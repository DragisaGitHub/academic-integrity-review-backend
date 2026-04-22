package com.academic.integrity.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.AnalysisStatus;
import com.academic.integrity.review.domain.Document;
import com.academic.integrity.review.domain.Finding;
import com.academic.integrity.review.domain.FindingCategory;
import com.academic.integrity.review.domain.FindingSeverity;
import com.academic.integrity.review.domain.ReviewPriority;
import com.academic.integrity.review.domain.ReviewStatus;
import com.academic.integrity.review.repository.AnalysisRepository;
import com.academic.integrity.review.repository.DocumentRepository;
import com.academic.integrity.review.repository.FindingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class FindingControllerIntegrationTest {

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

	@AfterEach
	void tearDown() {
		findingRepository.deleteAll();
		analysisRepository.deleteAll();
		documentRepository.deleteAll();
	}

	@Test
	void patchFindingPreflightIsAllowedForFrontendOrigin() throws Exception {
		Finding finding = createFinding();

		mockMvc.perform(options("/api/analyses/{analysisId}/findings/{findingId}",
						finding.getAnalysis().getId(), finding.getId())
				.header("Origin", "http://localhost:5173")
				.header("Access-Control-Request-Method", "PATCH")
				.header("Access-Control-Request-Headers", "content-type"))
				.andExpect(status().isOk())
				.andExpect(result -> {
					assertThat(result.getResponse().getHeader("Access-Control-Allow-Origin"))
							.isEqualTo("http://localhost:5173");
					assertThat(result.getResponse().getHeader("Access-Control-Allow-Methods"))
							.contains("PATCH");
				});
	}

	@Test
	void getFindingsReturnsInteractionFields() throws Exception {
		Finding finding = createFinding();

		String response = mockMvc.perform(get("/api/analyses/{analysisId}/findings", finding.getAnalysis().getId()))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode json = objectMapper.readTree(response);
		assertThat(json).hasSize(1);
		assertThat(json.get(0).path("professorNotes").isNull()).isTrue();
		assertThat(json.get(0).path("reviewed").asBoolean()).isFalse();
		assertThat(json.get(0).path("flaggedForFollowUp").asBoolean()).isFalse();
	}

	@Test
	void patchFindingUpdatesSingleField() throws Exception {
		Finding finding = createFinding();

		String response = mockMvc.perform(patch("/api/analyses/{analysisId}/findings/{findingId}",
						finding.getAnalysis().getId(), finding.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "professorNotes": "Needs a manual source audit."
						}
						"""))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		Finding updated = findingRepository.findById(finding.getId()).orElseThrow();
		assertThat(updated.getProfessorNotes()).isEqualTo("Needs a manual source audit.");
		assertThat(updated.isReviewed()).isFalse();
		assertThat(updated.isFlaggedForFollowUp()).isFalse();

		JsonNode json = objectMapper.readTree(response);
		assertThat(json.path("professorNotes").asText()).isEqualTo("Needs a manual source audit.");
		assertThat(json.path("reviewed").asBoolean()).isFalse();
		assertThat(json.path("flaggedForFollowUp").asBoolean()).isFalse();
	}

	@Test
	void patchFindingUpdatesBooleanCombination() throws Exception {
		Finding finding = createFinding();

		mockMvc.perform(patch("/api/analyses/{analysisId}/findings/{findingId}",
						finding.getAnalysis().getId(), finding.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "reviewed": true,
						  "flaggedForFollowUp": true
						}
						"""))
				.andExpect(status().isOk());

		Finding updated = findingRepository.findById(finding.getId()).orElseThrow();
		assertThat(updated.isReviewed()).isTrue();
		assertThat(updated.isFlaggedForFollowUp()).isTrue();
	}

	@Test
	void patchFindingReturnsNotFoundWhenAnalysisMissing() throws Exception {
		Finding finding = createFinding();

		mockMvc.perform(patch("/api/analyses/{analysisId}/findings/{findingId}", 999999L, finding.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void patchFindingReturnsNotFoundWhenFindingDoesNotBelongToAnalysis() throws Exception {
		Finding finding = createFinding();
		Finding anotherFinding = createFinding();

		mockMvc.perform(patch("/api/analyses/{analysisId}/findings/{findingId}",
						anotherFinding.getAnalysis().getId(), finding.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isNotFound());
	}

	private Finding createFinding() {
		Document document = new Document();
		document.setTitle("Analysis Paper");
		document.setStudentName("Jamie Student");
		document.setCourse("ENG-310");
		document.setSubmissionDate(LocalDate.now());
		document.setReviewPriority(ReviewPriority.MEDIUM);
		document.setReviewStatus(ReviewStatus.PENDING);
		document = documentRepository.saveAndFlush(document);

		Analysis analysis = new Analysis();
		analysis.setDocument(document);
		analysis.setAnalysisDate(LocalDate.now());
		analysis.setAnalysisStatus(AnalysisStatus.COMPLETED);
		analysis = analysisRepository.saveAndFlush(analysis);

		Finding finding = new Finding();
		finding.setAnalysis(analysis);
		finding.setCategory(FindingCategory.CITATION_ISSUE);
		finding.setSeverity(FindingSeverity.HIGH);
		finding.setTitle("Missing source attribution");
		finding.setExplanation("Claim lacks supporting citation.");
		finding.setExcerpt("Evidence excerpt");
		finding.setParagraphLocation("Paragraph 3");
		finding.setSuggestedAction("Add a source.");
		return findingRepository.saveAndFlush(finding);
	}
}