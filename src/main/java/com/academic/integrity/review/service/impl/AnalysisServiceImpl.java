package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.AnalysisStatus;
import com.academic.integrity.review.domain.Document;
import com.academic.integrity.review.dto.AnalysisNotesResponseDTO;
import com.academic.integrity.review.dto.AnalysisNotesUpsertRequestDTO;
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
import com.academic.integrity.review.service.AuthenticatedUserService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

	private final AnalysisRepository analysisRepository;
	private final DocumentRepository documentRepository;
	private final AnalysisMapper analysisMapper;
	private final AnalysisOrchestrationService analysisOrchestrationService;
	private final AuthenticatedUserService authenticatedUserService;

	@Override
	@Transactional(readOnly = true)
	public List<AnalysisResponseDTO> getAllAnalyses() {
		Long userId = authenticatedUserService.getAuthenticatedUserId();
		return analysisRepository.findAllByUser_Id(userId, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
				.map(analysisMapper::toDto)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public AnalysisResponseDTO getAnalysisByDocumentId(Long documentId) {
		Long userId = authenticatedUserService.getAuthenticatedUserId();
		return analysisMapper.toDto(
				analysisRepository
						.findByDocument_IdAndUser_Id(documentId, userId)
						.orElseThrow(() -> new ResourceNotFoundException("Analysis not found for documentId=" + documentId))
		);
	}

	@Override
	@Transactional(readOnly = true)
	public AnalysisStatusDTO getAnalysisStatus(Long analysisId) {
		Long userId = authenticatedUserService.getAuthenticatedUserId();
		Analysis analysis = analysisRepository.findByIdAndUser_Id(analysisId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Analysis not found: id=" + analysisId));
		return new AnalysisStatusDTO(analysis.getId(), analysis.getAnalysisStatus(), analysis.getErrorMessage());
	}

	@Override
	@Transactional(readOnly = true)
	public AnalysisNotesResponseDTO getAnalysisNotes(Long analysisId) {
		Long userId = authenticatedUserService.getAuthenticatedUserId();
		Analysis analysis = analysisRepository.findByIdAndUser_Id(analysisId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Analysis not found: id=" + analysisId));
		if (analysis.getAnalysisNotes() == null || analysis.getAnalysisNotes().isBlank()) {
			throw new ResourceNotFoundException("Analysis notes not found for analysis id=" + analysisId);
		}
		return new AnalysisNotesResponseDTO(analysis.getId(), analysis.getAnalysisNotes(), analysis.getUpdatedAt());
	}

	@Override
	public AnalysisResponseDTO createAnalysis(CreateAnalysisRequestDTO request) {
		if (request == null) {
			throw new IllegalArgumentException("Request body is required");
		}
		if (request.getDocumentId() == null) {
			throw new IllegalArgumentException("documentId is required");
		}

		Long triggeredByUserId = authenticatedUserService.getAuthenticatedUserId();

		Long documentId = request.getDocumentId();
		Document document = documentRepository.findByIdAndUser_Id(documentId, triggeredByUserId)
				.orElseThrow(() -> new ResourceNotFoundException("Document not found: id=" + documentId));

		analysisRepository.findByDocument_IdAndUser_Id(documentId, triggeredByUserId).ifPresent(existing -> {
			throw new DuplicateAnalysisException("Analysis already exists for documentId=" + documentId);
		});

		Analysis analysis = new Analysis();
		analysis.setUser(document.getUser());
		analysis.setDocument(document);
		analysis.setAnalysisDate(LocalDate.now());
		analysis.setAnalysisStatus(AnalysisStatus.PENDING);
		analysis.setFullText(null);
		analysis.setErrorMessage(null);

		Analysis saved = analysisRepository.saveAndFlush(analysis);
		analysisOrchestrationService.runAnalysis(saved.getId(), triggeredByUserId);
		return analysisMapper.toDto(saved);
	}

	@Override
	public AnalysisResponseDTO retryAnalysis(Long analysisId) {
		Long triggeredByUserId = authenticatedUserService.getAuthenticatedUserId();

		Analysis analysis = analysisRepository.findWithDocumentByIdAndUser_Id(analysisId, triggeredByUserId)
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

		analysisRepository.saveAndFlush(analysis);
		analysisOrchestrationService.runAnalysis(analysis.getId(), triggeredByUserId);
		return analysisMapper.toDto(analysis);
	}

	@Override
	@Transactional
	public AnalysisNotesResponseDTO upsertAnalysisNotes(Long analysisId, AnalysisNotesUpsertRequestDTO request) {
		if (request == null) {
			throw new IllegalArgumentException("Request body is required");
		}
		Long userId = authenticatedUserService.getAuthenticatedUserId();

		Analysis analysis = analysisRepository.findByIdAndUser_Id(analysisId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Analysis not found: id=" + analysisId));
		analysis.setAnalysisNotes(normalize(request.getNotes()));
		Analysis saved = analysisRepository.saveAndFlush(analysis);
		return new AnalysisNotesResponseDTO(saved.getId(), saved.getAnalysisNotes(), saved.getUpdatedAt());
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}