package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.AnalysisStatus;
import com.academic.integrity.review.domain.Document;
import com.academic.integrity.review.domain.FinalDecision;
import com.academic.integrity.review.domain.ReviewNote;
import com.academic.integrity.review.domain.ReviewPriority;
import com.academic.integrity.review.domain.ReviewStatus;
import com.academic.integrity.review.dto.DocumentResponseDTO;
import com.academic.integrity.review.dto.DocumentUploadRequestDTO;
import com.academic.integrity.review.exception.DocumentDeletionNotAllowedException;
import com.academic.integrity.review.exception.ResourceNotFoundException;
import com.academic.integrity.review.mapper.DocumentMapper;
import com.academic.integrity.review.repository.AnalysisRepository;
import com.academic.integrity.review.repository.DocumentRepository;
import com.academic.integrity.review.repository.FindingRepository;
import com.academic.integrity.review.repository.ReviewNoteRepository;
import com.academic.integrity.review.service.DocumentService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

	private static final Path UPLOADS_DIR = Paths.get("uploads");
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "txt");

	private final DocumentRepository documentRepository;
	private final AnalysisRepository analysisRepository;
	private final FindingRepository findingRepository;
	private final ReviewNoteRepository reviewNoteRepository;
	private final DocumentMapper documentMapper;

	@Override
	public List<DocumentResponseDTO> getAllDocuments() {
		return getDocuments(null, null, null, null, null, null);
	}

	@Override
	public List<DocumentResponseDTO> getDocuments(
			String course,
			String reviewPriority,
			String reviewStatus,
			String studentName,
			String sortBy,
			String sortDirection) {
		Sort sort = buildSort(sortBy, sortDirection);
		List<Document> documents = documentRepository.findAll(sort);

		String normalizedCourse = normalize(course);
		String normalizedStudentName = normalize(studentName);
		ReviewPriority parsedPriority = parseEnum(ReviewPriority.class, reviewPriority);
		ReviewStatus parsedStatus = parseEnum(ReviewStatus.class, reviewStatus);

		List<Document> filtered = documents.stream()
				.filter(doc -> normalizedCourse == null || doc.getCourse().equalsIgnoreCase(normalizedCourse))
				.filter(doc -> parsedPriority == null || doc.getReviewPriority() == parsedPriority)
				.filter(doc -> parsedStatus == null || doc.getReviewStatus() == parsedStatus)
				.filter(doc -> normalizedStudentName == null
						|| doc.getStudentName().toLowerCase(Locale.ROOT)
								.contains(normalizedStudentName.toLowerCase(Locale.ROOT)))
				.toList();

		List<DocumentResponseDTO> dtos = documentMapper.toDtoList(filtered);
		enrichDocumentDtos(dtos);
		return dtos;
	}

	@Override
	public DocumentResponseDTO getDocumentById(Long id) {
		Document document = documentRepository
				.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Document not found: id=" + id));

		DocumentResponseDTO dto = documentMapper.toDto(document);
		enrichDocumentDtos(List.of(dto));
		return dto;
	}

	@Override
	public DocumentResponseDTO uploadDocument(MultipartFile file, DocumentUploadRequestDTO request) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File is required");
		}
		validateFileType(file);
		String originalFilename = StringUtils.cleanPath(Objects.toString(file.getOriginalFilename(), ""));
		StoredFileInfo storedFile = storeFile(file);

		Document document = new Document();
		document.setTitle(normalizeRequired(request.getTitle(), "title"));
		document.setStudentName(normalizeRequired(request.getStudentName(), "studentName"));
		document.setCourse(normalizeRequired(request.getCourse(), "course"));
		document.setReviewPriority(request.getReviewPriority() != null ? request.getReviewPriority() : ReviewPriority.MEDIUM);
		document.setReviewStatus(ReviewStatus.PENDING);
		document.setSubmissionDate(LocalDate.now());
		document.setOriginalFilename(originalFilename.isBlank() ? null : originalFilename);
		document.setStoredFilename(storedFile.storedFilename());
		document.setStoredPath(storedFile.storedPath());
		document.setContentType(normalize(file.getContentType()));
		document.setFileSize(file.getSize());

		Document saved = documentRepository.save(document);
		DocumentResponseDTO dto = documentMapper.toDto(saved);
		enrichDocumentDtos(List.of(dto));
		return dto;
	}

	@Override
	@Transactional
	public void deleteDocument(Long id) {
		Document document = documentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Document not found: id=" + id));

		Analysis analysis = analysisRepository.findByDocument_Id(id).orElse(null);
		if (analysis != null && isDeleteBlocked(analysis.getAnalysisStatus())) {
			throw new DocumentDeletionNotAllowedException(
					"Document cannot be deleted while analysis is in progress");
		}

		String storedPath = normalize(document.getStoredPath());

		ReviewNote reviewNote = reviewNoteRepository.findByDocument_Id(id).orElse(null);
		if (reviewNote != null) {
			reviewNoteRepository.delete(reviewNote);
		}

		if (analysis != null) {
			findingRepository.deleteByAnalysis_Id(analysis.getId());
			analysisRepository.delete(analysis);
		}

		documentRepository.delete(document);
		documentRepository.flush();

		deleteStoredFile(storedPath);
	}

	private void enrichDocumentDtos(List<DocumentResponseDTO> dtos) {
		List<Long> documentIds = dtos.stream()
				.map(DocumentResponseDTO::getId)
				.filter(Objects::nonNull)
				.toList();
		if (documentIds.isEmpty()) {
			return;
		}

		Map<Long, Analysis> analysisByDocumentId = analysisRepository.findAllByDocument_IdIn(documentIds).stream()
				.filter(analysis -> analysis.getDocument() != null && analysis.getDocument().getId() != null)
				.collect(Collectors.toMap(
						analysis -> analysis.getDocument().getId(),
						Function.identity(),
						(existing, replacement) -> existing
				));

		Map<Long, ReviewNote> reviewNoteByDocumentId = reviewNoteRepository.findAllByDocument_IdIn(documentIds).stream()
				.filter(note -> note.getDocument() != null && note.getDocument().getId() != null)
				.collect(Collectors.toMap(
						note -> note.getDocument().getId(),
						Function.identity(),
						(existing, replacement) -> existing
				));

		for (DocumentResponseDTO dto : dtos) {
			Long documentId = dto.getId();
			if (documentId == null) {
				continue;
			}

			Analysis analysis = analysisByDocumentId.get(documentId);
			dto.setHasAnalysis(analysis != null);
			dto.setAnalysisId(analysis != null ? analysis.getId() : null);
			dto.setAnalysisStatus(analysis != null ? analysis.getAnalysisStatus() : null);
			dto.setAnalysisErrorMessage(analysis != null ? analysis.getErrorMessage() : null);

			ReviewNote note = reviewNoteByDocumentId.get(documentId);
			dto.setHasReviewNote(note != null);
			FinalDecision finalDecision = note != null ? note.getFinalDecision() : null;
			dto.setFinalDecision(finalDecision);
		}
	}

	private static void validateFileType(MultipartFile file) {
		String originalFilename = StringUtils.cleanPath(Objects.toString(file.getOriginalFilename(), ""));
		String extension = getExtension(originalFilename);
		if (!ALLOWED_EXTENSIONS.contains(extension)) {
			throw new IllegalArgumentException("Unsupported file type. Allowed: PDF, DOCX, TXT");
		}
	}

	private record StoredFileInfo(String storedFilename, String storedPath) {
	}

	private static StoredFileInfo storeFile(MultipartFile file) {
		String originalFilename = StringUtils.cleanPath(Objects.toString(file.getOriginalFilename(), ""));
		if (originalFilename.isBlank()) {
			throw new IllegalArgumentException("Original filename is required");
		}
		String safeFilename = originalFilename.replace("\\", "_").replace("/", "_");
		String storedFilename = System.currentTimeMillis() + "-" + safeFilename;
		Path target = UPLOADS_DIR.resolve(storedFilename);
		try {
			Files.createDirectories(UPLOADS_DIR);
			file.transferTo(target);
		} catch (IOException ex) {
			throw new IllegalArgumentException("Failed to store file", ex);
		}

		String storedPath = UPLOADS_DIR.resolve(storedFilename).toString().replace("\\", "/");
		return new StoredFileInfo(storedFilename, storedPath);
	}

	private static String getExtension(String filename) {
		int lastDot = filename.lastIndexOf('.');
		if (lastDot < 0 || lastDot == filename.length() - 1) {
			return "";
		}
		return filename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
	}

	private static String normalizeRequired(String value, String fieldName) {
		String normalized = normalize(value);
		if (normalized == null) {
			throw new IllegalArgumentException(fieldName + " is required");
		}
		return normalized;
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static <T extends Enum<T>> T parseEnum(Class<T> enumClass, String raw) {
		String normalized = normalize(raw);
		if (normalized == null) {
			return null;
		}
		try {
			return Enum.valueOf(enumClass, normalized.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	private static Sort buildSort(String sortBy, String sortDirection) {
		String normalizedSortBy = normalize(sortBy);
		String normalizedSortDirection = normalize(sortDirection);

		Set<String> allowedSortFields = Set.of("submissionDate", "title", "createdAt");
		String sortField = (normalizedSortBy != null && allowedSortFields.contains(normalizedSortBy))
				? normalizedSortBy
				: "submissionDate";

		Sort.Direction direction = Sort.Direction.DESC;
		if (Objects.equals(normalizedSortDirection, "asc")) {
			direction = Sort.Direction.ASC;
		}

		return Sort.by(direction, sortField);
	}

	private static boolean isDeleteBlocked(AnalysisStatus status) {
		return status == AnalysisStatus.PENDING
				|| status == AnalysisStatus.EXTRACTING
				|| status == AnalysisStatus.ANALYZING;
	}

	private static void deleteStoredFile(String storedPath) {
		if (storedPath == null) {
			return;
		}

		Path path = Paths.get(storedPath);
		try {
			Files.deleteIfExists(path);
		} catch (IOException ex) {
			throw new IllegalStateException("Failed to delete stored file: " + path, ex);
		}
	}
}