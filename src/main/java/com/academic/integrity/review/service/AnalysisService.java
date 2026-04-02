package com.academic.integrity.review.service;

import com.academic.integrity.review.dto.AnalysisResponseDTO;
import com.academic.integrity.review.dto.AnalysisStatusDTO;
import com.academic.integrity.review.dto.CreateAnalysisRequestDTO;

public interface AnalysisService {

	AnalysisResponseDTO getAnalysisByDocumentId(Long documentId);

	AnalysisStatusDTO getAnalysisStatus(Long analysisId);

	AnalysisResponseDTO createAnalysis(CreateAnalysisRequestDTO request);

	AnalysisResponseDTO retryAnalysis(Long analysisId);
}
