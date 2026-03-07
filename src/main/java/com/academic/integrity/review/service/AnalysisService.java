package com.academic.integrity.review.service;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.Document;
import com.academic.integrity.review.dto.AnalysisResponseDTO;
import com.academic.integrity.review.dto.CreateAnalysisRequestDTO;
import com.academic.integrity.review.exception.DuplicateAnalysisException;
import com.academic.integrity.review.exception.ResourceNotFoundException;
import com.academic.integrity.review.mapper.AnalysisMapper;
import com.academic.integrity.review.repository.AnalysisRepository;
import com.academic.integrity.review.repository.DocumentRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysisService {

	private final AnalysisRepository analysisRepository;
	private final DocumentRepository documentRepository;
	private final AnalysisMapper analysisMapper;

	@Transactional(readOnly = true)
	public AnalysisResponseDTO getAnalysisByDocumentId(Long documentId) {
		return analysisMapper.toDto(
				analysisRepository
						.findByDocument_Id(documentId)
						.orElseThrow(() -> new ResourceNotFoundException("Analysis not found for documentId=" + documentId))
		);
	}

	@Transactional
	public AnalysisResponseDTO createAnalysis(CreateAnalysisRequestDTO request) {
		if (request == null) {
			throw new IllegalArgumentException("Request body is required");
		}
		if (request.getDocumentId() == null) {
			throw new IllegalArgumentException("documentId is required");
		}
		if (request.getFullText() == null || request.getFullText().isBlank()) {
			throw new IllegalArgumentException("fullText is required");
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
		analysis.setFullText(request.getFullText());

		Analysis saved = analysisRepository.save(analysis);
		return analysisMapper.toDto(saved);
	}
}
