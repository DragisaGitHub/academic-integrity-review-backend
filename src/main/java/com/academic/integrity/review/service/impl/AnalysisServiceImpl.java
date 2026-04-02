package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.AnalysisStatus;
import com.academic.integrity.review.domain.Document;
import com.academic.integrity.review.dto.AnalysisResponseDTO;
import com.academic.integrity.review.dto.AnalysisStatusDTO;
import com.academic.integrity.review.dto.CreateAnalysisRequestDTO;
import com.academic.integrity.review.exception.AnalysisRetryNotAllowedException;
import com.academic.integrity.review.exception.DuplicateAnalysisException;
import com.academic.integrity.review.exception.ResourceNotFoundException;
import com.academic.integrity.review.mapper.AnalysisMapper;
import com.academic.integrity.review.repository.AnalysisRepository;
import com.academic.integrity.review.repository.DocumentRepository;
import com.academic.integrity.review.service.AnalysisOrchestrationService;
import com.academic.integrity.review.service.AnalysisService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

	private final AnalysisRepository analysisRepository;
	private final DocumentRepository documentRepository;
	private final AnalysisMapper analysisMapper;
	private final AnalysisOrchestrationService analysisOrchestrationService;

	@Override
	@Transactional(readOnly = true)
	public AnalysisResponseDTO getAnalysisByDocumentId(Long documentId) {
		return analysisMapper.toDto(
				analysisRepository
						.findByDocument_Id(documentId)
						.orElseThrow(() -> new ResourceNotFoundException("Analysis not found for documentId=" + documentId))
		);
	}

	@Override
	@Transactional(readOnly = true)
	public AnalysisStatusDTO getAnalysisStatus(Long analysisId) {
		Analysis analysis = analysisRepository.findById(analysisId)
				.orElseThrow(() -> new ResourceNotFoundException("Analysis not found: id=" + analysisId));
		return new AnalysisStatusDTO(analysis.getId(), analysis.getAnalysisStatus(), analysis.getErrorMessage());
	}

	@Override
	public AnalysisResponseDTO createAnalysis(CreateAnalysisRequestDTO request) {
		if (request == null) {
			throw new IllegalArgumentException("Request body is required");
		}
		if (request.getDocumentId() == null) {
			throw new IllegalArgumentException("documentId is required");
		}

		Long documentId = request.getDocumentId();
		Document document = documentRepository.findById(documentId)
				.orElseThrow(() -> new ResourceNotFoundException("Document not found: id=" + documentId));

		analysisRepository.findByDocument_Id(documentId).ifPresent(existing -> {
			throw new DuplicateAnalysisException("Analysis already exists for documentId=" + documentId);
		});

		Analysis analysis = new Analysis();
		analysis.setDocument(document);
		analysis.setAnalysisDate(LocalDate.now());
		analysis.setAnalysisStatus(AnalysisStatus.PENDING);
		analysis.setFullText(null);
		analysis.setErrorMessage(null);

		Analysis saved = analysisRepository.saveAndFlush(analysis);
		analysisOrchestrationService.runAnalysis(saved.getId());
		return analysisMapper.toDto(saved);
	}

	@Override
	public AnalysisResponseDTO retryAnalysis(Long analysisId) {
		Analysis analysis = analysisRepository.findById(analysisId)
				.orElseThrow(() -> new ResourceNotFoundException("Analysis not found: id=" + analysisId));

		if (analysis.getAnalysisStatus() != AnalysisStatus.FAILED) {
			throw new AnalysisRetryNotAllowedException(
					"Analysis retry is only allowed when status is FAILED");
		}

		analysis.setAnalysisStatus(AnalysisStatus.PENDING);
		analysis.setErrorMessage(null);
		analysis.setStartedAt(null);
		analysis.setCompletedAt(null);
		analysis.setModelName(null);
		analysis.setTotalTokensUsed(null);

		Analysis saved = analysisRepository.saveAndFlush(analysis);
		analysisOrchestrationService.runAnalysis(saved.getId());
		return analysisMapper.toDto(saved);
	}
}