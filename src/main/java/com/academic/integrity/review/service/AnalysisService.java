package com.academic.integrity.review.service;

import com.academic.integrity.review.dto.AnalysisNotesResponseDTO;
import com.academic.integrity.review.dto.AnalysisNotesUpsertRequestDTO;
import com.academic.integrity.review.dto.AnalysisResponseDTO;
import com.academic.integrity.review.dto.AnalysisStatusDTO;
import com.academic.integrity.review.dto.CreateAnalysisRequestDTO;
import java.util.List;

public interface AnalysisService {

	List<AnalysisResponseDTO> getAllAnalyses();

	AnalysisResponseDTO getAnalysisByDocumentId(Long documentId);

	AnalysisStatusDTO getAnalysisStatus(Long analysisId);

	AnalysisNotesResponseDTO getAnalysisNotes(Long analysisId);

	AnalysisResponseDTO createAnalysis(CreateAnalysisRequestDTO request);

	AnalysisResponseDTO retryAnalysis(Long analysisId);

	AnalysisNotesResponseDTO upsertAnalysisNotes(Long analysisId, AnalysisNotesUpsertRequestDTO request);
}
