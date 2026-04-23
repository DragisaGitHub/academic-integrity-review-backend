package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.domain.Document;
import com.academic.integrity.review.domain.ReviewNote;
import com.academic.integrity.review.domain.ReviewStatus;
import com.academic.integrity.review.dto.ReviewNoteResponseDTO;
import com.academic.integrity.review.dto.ReviewNoteUpsertRequestDTO;
import com.academic.integrity.review.exception.ResourceNotFoundException;
import com.academic.integrity.review.mapper.ReviewNoteMapper;
import com.academic.integrity.review.repository.DocumentRepository;
import com.academic.integrity.review.repository.ReviewNoteRepository;
import com.academic.integrity.review.service.AuthenticatedUserService;
import com.academic.integrity.review.service.ReviewNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewNoteServiceImpl implements ReviewNoteService {

	private final DocumentRepository documentRepository;
	private final ReviewNoteRepository reviewNoteRepository;
	private final ReviewNoteMapper reviewNoteMapper;
	private final AuthenticatedUserService authenticatedUserService;

	@Override
	@Transactional(readOnly = true)
	public ReviewNoteResponseDTO getByDocumentId(Long documentId) {
		Long userId = authenticatedUserService.getAuthenticatedUserId();
		Document document = documentRepository.findByIdAndUser_Id(documentId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

		ReviewNote reviewNote = reviewNoteRepository.findByDocument_IdAndUser_Id(document.getId(), userId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Review note not found for document id: " + documentId));

		return reviewNoteMapper.toDto(reviewNote);
	}

	@Override
	@Transactional
	public ReviewNoteResponseDTO upsertByDocumentId(Long documentId, ReviewNoteUpsertRequestDTO request) {
		Long userId = authenticatedUserService.getAuthenticatedUserId();
		Document document = documentRepository.findByIdAndUser_Id(documentId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

		ReviewNote reviewNote = reviewNoteRepository.findByDocument_IdAndUser_Id(document.getId(), userId).orElseGet(() -> {
			ReviewNote created = new ReviewNote();
			created.setUser(document.getUser());
			created.setDocument(document);
			return created;
		});

		reviewNote.setNotes(request.getNotes());
		reviewNote.setReferencesChecked(request.isReferencesChecked());
		reviewNote.setOralDefenseRequired(request.isOralDefenseRequired());
		reviewNote.setFactualIssuesDiscussed(request.isFactualIssuesDiscussed());
		reviewNote.setFinalReviewCompleted(request.isFinalReviewCompleted());
		reviewNote.setFinalDecision(request.getFinalDecision());
		document.setReviewStatus(resolveReviewStatus(reviewNote));

		ReviewNote saved = reviewNoteRepository.save(reviewNote);
		return reviewNoteMapper.toDto(saved);
	}

	private static ReviewStatus resolveReviewStatus(ReviewNote reviewNote) {
		return reviewNote.isFinalReviewCompleted()
				? ReviewStatus.REVIEWED
				: ReviewStatus.IN_REVIEW;
	}
}