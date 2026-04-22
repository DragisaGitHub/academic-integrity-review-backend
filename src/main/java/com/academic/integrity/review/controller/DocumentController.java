package com.academic.integrity.review.controller;

import com.academic.integrity.review.dto.DocumentUpdateRequestDTO;
import com.academic.integrity.review.dto.DocumentResponseDTO;
import com.academic.integrity.review.dto.DocumentUploadRequestDTO;
import com.academic.integrity.review.service.DocumentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

	private final DocumentService documentService;

	@GetMapping
	public List<DocumentResponseDTO> getAllDocuments(
			@RequestParam(required = false) String course,
			@RequestParam(required = false) String reviewPriority,
			@RequestParam(required = false) String reviewStatus,
			@RequestParam(required = false) String studentName,
			@RequestParam(required = false) String sortBy,
			@RequestParam(required = false) String sortDirection) {
		return documentService.getDocuments(course, reviewPriority, reviewStatus, studentName, sortBy, sortDirection);
	}

	@GetMapping("/{id}")
	public DocumentResponseDTO getDocumentById(@PathVariable Long id) {
		return documentService.getDocumentById(id);
	}

	@GetMapping(path = "/export", produces = "text/csv")
	public ResponseEntity<String> exportDocuments() {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=documents-export.csv")
				.contentType(MediaType.parseMediaType("text/csv"))
				.body(documentService.exportDocumentsCsv());
	}

	@PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public DocumentResponseDTO uploadDocument(
			@RequestParam("file") MultipartFile file,
			@ModelAttribute DocumentUploadRequestDTO request) {
		return documentService.uploadDocument(file, request);
	}

	@PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public DocumentResponseDTO updateDocument(
			@PathVariable Long id,
			@RequestBody DocumentUpdateRequestDTO request) {
		return documentService.updateDocument(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
		documentService.deleteDocument(id);
		return ResponseEntity.noContent().build();
	}
}
