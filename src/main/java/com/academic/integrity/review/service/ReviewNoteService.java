package com.academic.integrity.review.service;

import com.academic.integrity.review.dto.ReviewNoteResponseDTO;
import com.academic.integrity.review.dto.ReviewNoteUpsertRequestDTO;
public interface ReviewNoteService {

	ReviewNoteResponseDTO getByDocumentId(Long documentId);

	ReviewNoteResponseDTO upsertByDocumentId(Long documentId, ReviewNoteUpsertRequestDTO request);
}
