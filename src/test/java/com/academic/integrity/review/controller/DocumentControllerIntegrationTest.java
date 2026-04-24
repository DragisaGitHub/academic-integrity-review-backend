package com.academic.integrity.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.academic.integrity.review.domain.User;
import com.academic.integrity.review.domain.UserRole;
import com.academic.integrity.review.domain.TextSegment;
import com.academic.integrity.review.repository.AnalysisRepository;
import com.academic.integrity.review.repository.DocumentRepository;
import com.academic.integrity.review.repository.FindingRepository;
import com.academic.integrity.review.repository.ReviewNoteRepository;
import com.academic.integrity.review.repository.TextSegmentRepository;
import com.academic.integrity.review.repository.UserRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
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

	@Autowired
	private TextSegmentRepository textSegmentRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@TempDir
	Path tempDir;

	@AfterEach
	void tearDown() {
		findingRepository.deleteAll();
		textSegmentRepository.deleteAll();
		analysisRepository.deleteAll();
		reviewNoteRepository.deleteAll();
		documentRepository.deleteAll();
	}

	@Test
	void uploadDocumentPersistsAcademicYearAndReturnsIt() throws Exception {
		User admin = ensureUser("admin", UserRole.ADMIN);
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"essay.txt",
				MediaType.TEXT_PLAIN_VALUE,
				"Academic essay content".getBytes());

		String response = mockMvc.perform(multipart("/api/documents/upload")
						.file(file)
						.param("title", "Ethics Essay")
						.param("studentName", "Jordan Student")
						.param("course", "PHIL-101")
						.param("academicYear", "2025-2026")
						.param("reviewPriority", "HIGH"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode json = objectMapper.readTree(response);
		Long documentId = json.path("id").asLong();

		assertThat(json.path("academicYear").asText()).isEqualTo("2025-2026");
		Document saved = documentRepository.findById(documentId).orElseThrow();
		assertThat(saved.getUser().getId()).isEqualTo(admin.getId());
		assertThat(saved.getAcademicYear()).isEqualTo("2025-2026");
	}

	@Test
	void getAndListDocumentsIncludeAcademicYear() throws Exception {
		User admin = ensureUser("admin", UserRole.ADMIN);
		Document document = new Document();
		document.setUser(admin);
		document.setTitle("Policy Review");
		document.setStudentName("Sam Student");
		document.setCourse("LAW-210");
		document.setAcademicYear("2024-2025");
		document.setSubmissionDate(LocalDate.now());
		document.setReviewPriority(ReviewPriority.MEDIUM);
		document.setReviewStatus(ReviewStatus.PENDING);
		document = documentRepository.saveAndFlush(document);

		String getResponse = mockMvc.perform(get("/api/documents/{id}", document.getId()))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode getJson = objectMapper.readTree(getResponse);
		assertThat(getJson.path("academicYear").asText()).isEqualTo("2024-2025");

		String listResponse = mockMvc.perform(get("/api/documents"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode listJson = objectMapper.readTree(listResponse);
		assertThat(listJson).hasSize(1);
		assertThat(listJson.get(0).path("academicYear").asText()).isEqualTo("2024-2025");
	}

	@Test
	void updateDocumentUpdatesAcademicYearAndEditableFields() throws Exception {
		User admin = ensureUser("admin", UserRole.ADMIN);
		Document document = new Document();
		document.setUser(admin);
		document.setTitle("Initial Title");
		document.setStudentName("Casey Student");
		document.setCourse("ENG-100");
		document.setAcademicYear("2023-2024");
		document.setSubmissionDate(LocalDate.now());
		document.setReviewPriority(ReviewPriority.LOW);
		document.setReviewStatus(ReviewStatus.PENDING);
		document = documentRepository.saveAndFlush(document);

		String request = """
				{
				  "title": "Updated Title",
				  "studentName": "Casey Scholar",
				  "course": "ENG-200",
				  "academicYear": "2025-2026",
				  "reviewPriority": "HIGH"
				}
				""";

		String response = mockMvc.perform(put("/api/documents/{id}", document.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		Document updated = documentRepository.findById(document.getId()).orElseThrow();
		assertThat(updated.getTitle()).isEqualTo("Updated Title");
		assertThat(updated.getStudentName()).isEqualTo("Casey Scholar");
		assertThat(updated.getCourse()).isEqualTo("ENG-200");
		assertThat(updated.getAcademicYear()).isEqualTo("2025-2026");
		assertThat(updated.getReviewPriority()).isEqualTo(ReviewPriority.HIGH);

		JsonNode json = objectMapper.readTree(response);
		assertThat(json.path("academicYear").asText()).isEqualTo("2025-2026");
		assertThat(json.path("title").asText()).isEqualTo("Updated Title");
	}

	@Test
	void updateDocumentReturnsNotFoundWhenMissing() throws Exception {
		mockMvc.perform(put("/api/documents/{id}", 999999L)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
							{
							  "title": "Updated Title",
							  "studentName": "Casey Scholar",
							  "course": "ENG-200",
							  "academicYear": "2025-2026",
							  "reviewPriority": "HIGH"
							}
							"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void exportDocumentsReturnsCsvWithAcademicYear() throws Exception {
		User admin = ensureUser("admin", UserRole.ADMIN);
		Document document = new Document();
		document.setUser(admin);
		document.setTitle("Export Title");
		document.setStudentName("Morgan Student");
		document.setCourse("HIST-220");
		document.setAcademicYear("2025-2026");
		document.setSubmissionDate(LocalDate.of(2026, 4, 3));
		document.setReviewPriority(ReviewPriority.HIGH);
		document.setReviewStatus(ReviewStatus.REVIEWED);
		document = documentRepository.saveAndFlush(document);

		String response = mockMvc.perform(get("/api/documents/export"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(response).contains("academicYear");
		assertThat(response).contains("\"Export Title\"");
		assertThat(response).contains("\"2025-2026\"");
		assertThat(response).contains("\"REVIEWED\"");
	}

	@Test
	void deleteDocumentRemovesAggregateAndStoredFile() throws Exception {
		User admin = ensureUser("admin", UserRole.ADMIN);
		Path file = tempDir.resolve("history-paper.txt");
		Files.writeString(file, "Stored paper content");

		Document document = new Document();
		document.setUser(admin);
		document.setTitle("History Entry");
		document.setStudentName("Taylor Student");
		document.setCourse("ENG-201");
		document.setAcademicYear("2024-2025");
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
		analysis.setUser(admin);
		analysis.setDocument(document);
		analysis.setAnalysisDate(LocalDate.now());
		analysis.setAnalysisStatus(AnalysisStatus.COMPLETED);
		analysis.setFullText("Extracted text");
		analysis = analysisRepository.saveAndFlush(analysis);

		TextSegment segment = new TextSegment();
		segment.setAnalysis(analysis);
		segment.setSegmentIndex(0);
		segment.setContent("Extracted text");
		segment.setStartOffset(0);
		segment.setEndOffset("Extracted text".length());
		textSegmentRepository.saveAndFlush(segment);

		Finding finding = new Finding();
		finding.setAnalysis(analysis);
		finding.setCategory(FindingCategory.CITATION_ISSUE);
		finding.setSeverity(FindingSeverity.MEDIUM);
		finding.setTitle("Example finding");
		findingRepository.saveAndFlush(finding);

		ReviewNote reviewNote = new ReviewNote();
		reviewNote.setUser(admin);
		reviewNote.setDocument(document);
		reviewNote.setNotes("Reviewed");
		reviewNoteRepository.saveAndFlush(reviewNote);

		mockMvc.perform(delete("/api/documents/{id}", document.getId()))
				.andExpect(status().isNoContent());

		assertThat(documentRepository.findById(document.getId())).isEmpty();
		assertThat(analysisRepository.findById(analysis.getId())).isEmpty();
		assertThat(findingRepository.findAll()).isEmpty();
		assertThat(textSegmentRepository.findAll()).isEmpty();
		assertThat(reviewNoteRepository.findAll()).isEmpty();
		assertThat(Files.exists(file)).isFalse();
	}

	@Test
	void deleteDocumentReturnsConflictWhenAnalysisIsRunning() throws Exception {
		User admin = ensureUser("admin", UserRole.ADMIN);
		Path file = tempDir.resolve("in-progress-paper.txt");
		Files.writeString(file, "Stored paper content");

		Document document = new Document();
		document.setUser(admin);
		document.setTitle("Running Analysis Entry");
		document.setStudentName("Alex Student");
		document.setCourse("ENG-202");
		document.setAcademicYear("2024-2025");
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
		analysis.setUser(admin);
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

	@Test
	void uploadedDocumentIsIsolatedFromOtherUsers() throws Exception {
		ensureUser("user-a", UserRole.USER);
		ensureUser("user-b", UserRole.USER);

		MockMultipartFile file = new MockMultipartFile(
				"file",
				"essay.txt",
				MediaType.TEXT_PLAIN_VALUE,
				"Academic essay content".getBytes());

		String uploadResponse = mockMvc.perform(multipart("/api/documents/upload")
						.file(file)
						.with(user("user-a").roles("USER"))
						.param("title", "Private Essay")
						.param("studentName", "Jordan Student")
						.param("course", "PHIL-101")
						.param("academicYear", "2025-2026")
						.param("reviewPriority", "HIGH"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		Long documentId = objectMapper.readTree(uploadResponse).path("id").asLong();

		String userAList = mockMvc.perform(get("/api/documents").with(user("user-a").roles("USER")))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(objectMapper.readTree(userAList)).hasSize(1);

		String userBList = mockMvc.perform(get("/api/documents").with(user("user-b").roles("USER")))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(objectMapper.readTree(userBList)).isEmpty();

		mockMvc.perform(get("/api/documents/{id}", documentId).with(user("user-b").roles("USER")))
				.andExpect(status().isNotFound());

		String userBExport = mockMvc.perform(get("/api/documents/export").with(user("user-b").roles("USER")))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(userBExport).doesNotContain("Private Essay");
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