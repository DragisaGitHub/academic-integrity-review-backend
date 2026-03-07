package com.academic.integrity.review.controller;

import com.academic.integrity.review.dto.ReviewNoteResponseDTO;
import com.academic.integrity.review.dto.ReviewNoteUpsertRequestDTO;
import com.academic.integrity.review.service.ReviewNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents/{documentId}/review-note")
@RequiredArgsConstructor
public class ReviewNoteController {

	private final ReviewNoteService reviewNoteService;

	@GetMapping
	public ResponseEntity<ReviewNoteResponseDTO> getByDocumentId(@PathVariable Long documentId) {
		return ResponseEntity.ok(reviewNoteService.getByDocumentId(documentId));
	}

	@PostMapping
	public ResponseEntity<ReviewNoteResponseDTO> upsertByDocumentId(
			@PathVariable Long documentId,
			@RequestBody ReviewNoteUpsertRequestDTO request) {
		return ResponseEntity.ok(reviewNoteService.upsertByDocumentId(documentId, request));
	}
}
