package com.academic.integrity.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.AnalysisStatus;
import com.academic.integrity.review.domain.Document;
import com.academic.integrity.review.domain.ReviewPriority;
import com.academic.integrity.review.domain.ReviewStatus;
import com.academic.integrity.review.repository.AnalysisRepository;
import com.academic.integrity.review.repository.DocumentRepository;
import com.academic.integrity.review.repository.FindingRepository;
import com.academic.integrity.review.service.LlmClientService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
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

	@MockBean
	private LlmClientService llmClientService;

	@TempDir
	Path tempDir;

	@AfterEach
	void tearDown() {
		findingRepository.deleteAll();
		analysisRepository.deleteAll();
		documentRepository.deleteAll();
	}

	@Test
	void createAnalysisRunsAsyncAndPersistsFindings() throws Exception {
		Document document = createDocument("This seminar paper makes unsupported claims without sources.");
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
				.contentType(MediaType.APPLICATION_JSON)
				.content("{" + "\"documentId\":" + document.getId() + "}"))
				.andExpect(status().isAccepted())
				.andReturn();

		Long analysisId = responseAnalysisId(result);
		awaitStatus(analysisId, AnalysisStatus.COMPLETED);

		Analysis analysis = analysisRepository.findById(analysisId).orElseThrow();
		assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
		assertThat(analysis.getModelName()).isEqualTo("test-model");
		assertThat(analysis.getTotalTokensUsed()).isEqualTo(321);
		assertThat(analysis.getFullText()).contains("unsupported claims");
		assertThat(findingRepository.findAllByAnalysis_Id(analysisId)).hasSize(1);

		String documentBody = mockMvc.perform(get("/api/documents/{id}", document.getId()))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode documentJson = objectMapper.readTree(documentBody);
		assertThat(documentJson.path("hasAnalysis").asBoolean()).isTrue();
		assertThat(documentJson.path("analysisId").asLong()).isEqualTo(analysisId);
		assertThat(documentJson.path("analysisStatus").asText()).isEqualTo("COMPLETED");
		assertThat(documentJson.path("analysisErrorMessage").isNull()).isTrue();

		mockMvc.perform(get("/api/analyses/{analysisId}/status", analysisId))
				.andExpect(status().isOk());
	}

	@Test
	void retryAnalysisRequeuesFailedAnalysis() throws Exception {
		Document document = createDocument("A short document for retry testing.");
		doThrow(new IllegalStateException("Simulated OpenAI failure"))
				.when(llmClientService).analyze(anyString());

		MvcResult initial = mockMvc.perform(post("/api/analyses")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{" + "\"documentId\":" + document.getId() + "}"))
				.andExpect(status().isAccepted())
				.andReturn();

		Long analysisId = responseAnalysisId(initial);
		awaitStatus(analysisId, AnalysisStatus.FAILED);

		Analysis failed = analysisRepository.findById(analysisId).orElseThrow();
		assertThat(failed.getErrorMessage()).contains("Simulated OpenAI failure");

		reset(llmClientService);
		when(llmClientService.analyze(anyString())).thenReturn(new LlmClientService.LlmAnalysisResult(
				"{\"findings\":[]}",
				"retry-model",
				111
		));

		mockMvc.perform(post("/api/analyses/{analysisId}/retry", analysisId))
				.andExpect(status().isAccepted());

		awaitStatus(analysisId, AnalysisStatus.COMPLETED);
		Analysis completed = analysisRepository.findById(analysisId).orElseThrow();
		assertThat(completed.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
		assertThat(completed.getErrorMessage()).isNull();
	}

	private Document createDocument(String content) throws Exception {
		Path file = tempDir.resolve("paper.txt");
		Files.writeString(file, content);

		Document document = new Document();
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
}