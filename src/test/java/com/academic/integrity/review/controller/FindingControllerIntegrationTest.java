package com.academic.integrity.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
import com.academic.integrity.review.domain.User;
import com.academic.integrity.review.domain.UserRole;
import com.academic.integrity.review.repository.AnalysisRepository;
import com.academic.integrity.review.repository.DocumentRepository;
import com.academic.integrity.review.repository.FindingRepository;
import com.academic.integrity.review.repository.TextSegmentRepository;
import com.academic.integrity.review.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

	@Autowired
	private TextSegmentRepository textSegmentRepository;

	@Autowired
	private UserRepository userRepository;

	@AfterEach
	void tearDown() {
		findingRepository.deleteAll();
		textSegmentRepository.deleteAll();
		analysisRepository.deleteAll();
		documentRepository.deleteAll();
	}

	@Test
	void patchFindingPreflightIsAllowedForFrontendOrigin() throws Exception {
		Finding finding = createFinding(ensureUser("admin", UserRole.ADMIN));

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
		Finding finding = createFinding(ensureUser("admin", UserRole.ADMIN));

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
		assertThat(json.get(0).path("segmentIndex").asInt()).isEqualTo(1);
		assertThat(json.get(0).path("excerptStartOffset").asInt()).isGreaterThanOrEqualTo(0);
		assertThat(json.get(0).path("excerptEndOffset").asInt())
				.isGreaterThan(json.get(0).path("excerptStartOffset").asInt());
		Finding anchoredFinding = findingRepository.findById(finding.getId()).orElseThrow();
		assertThat(anchoredFinding.getSegmentIndex()).isEqualTo(1);
		assertThat(textSegmentRepository.countByAnalysis_Id(finding.getAnalysis().getId())).isEqualTo(3);
	}

	@Test
	void patchFindingUpdatesSingleField() throws Exception {
		Finding finding = createFinding(ensureUser("admin", UserRole.ADMIN));

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
		Finding finding = createFinding(ensureUser("admin", UserRole.ADMIN));

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
		Finding finding = createFinding(ensureUser("admin", UserRole.ADMIN));

		mockMvc.perform(patch("/api/analyses/{analysisId}/findings/{findingId}", 999999L, finding.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void patchFindingReturnsNotFoundWhenFindingDoesNotBelongToAnalysis() throws Exception {
		Finding finding = createFinding(ensureUser("admin", UserRole.ADMIN));
		Finding anotherFinding = createFinding(ensureUser("admin-two", UserRole.ADMIN));

		mockMvc.perform(patch("/api/analyses/{analysisId}/findings/{findingId}",
						anotherFinding.getAnalysis().getId(), finding.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void findingsAreScopedToOwningUser() throws Exception {
		User userA = ensureUser("user-a", UserRole.USER);
		ensureUser("user-b", UserRole.USER);
		Finding finding = createFinding(userA);

		mockMvc.perform(get("/api/analyses/{analysisId}/findings", finding.getAnalysis().getId())
						.with(user("user-b").roles("USER")))
				.andExpect(status().isNotFound());

		mockMvc.perform(patch("/api/analyses/{analysisId}/findings/{findingId}",
						finding.getAnalysis().getId(), finding.getId())
						.with(user("user-b").roles("USER"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isNotFound());
	}

	private Finding createFinding(User owner) {
		Document document = new Document();
		document.setUser(owner);
		document.setTitle("Analysis Paper");
		document.setStudentName("Jamie Student");
		document.setCourse("ENG-310");
		document.setSubmissionDate(LocalDate.now());
		document.setReviewPriority(ReviewPriority.MEDIUM);
		document.setReviewStatus(ReviewStatus.PENDING);
		document = documentRepository.saveAndFlush(document);

		Analysis analysis = new Analysis();
		analysis.setUser(owner);
		analysis.setDocument(document);
		analysis.setAnalysisDate(LocalDate.now());
		analysis.setAnalysisStatus(AnalysisStatus.COMPLETED);
		analysis.setFullText(
				"Opening paragraph with enough detail for segmentation tests."
						+ "\n\nEvidence excerpt appears inside the second paragraph for anchor matching."
						+ "\n\nClosing paragraph with additional discussion.");
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