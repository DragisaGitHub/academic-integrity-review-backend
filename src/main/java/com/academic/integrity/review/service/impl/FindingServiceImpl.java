package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.domain.Finding;
import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.dto.FindingResponseDTO;
import com.academic.integrity.review.dto.FindingUpdateRequestDTO;
import com.academic.integrity.review.exception.ResourceNotFoundException;
import com.academic.integrity.review.mapper.FindingMapper;
import com.academic.integrity.review.repository.AnalysisRepository;
import com.academic.integrity.review.repository.FindingRepository;
import com.academic.integrity.review.service.AuthenticatedUserService;
import com.academic.integrity.review.service.FindingAnchorService;
import com.academic.integrity.review.service.FindingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindingServiceImpl implements FindingService {

	private final AnalysisRepository analysisRepository;
	private final FindingRepository findingRepository;
	private final FindingMapper findingMapper;
	private final AuthenticatedUserService authenticatedUserService;
	private final FindingAnchorService findingAnchorService;

	@Override
	@Transactional
	public List<FindingResponseDTO> getFindingsByAnalysisId(Long analysisId) {
		Long userId = authenticatedUserService.getAuthenticatedUserId();
		Analysis analysis = analysisRepository.findByIdAndUser_Id(analysisId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Analysis not found: id=" + analysisId));
		findingAnchorService.backfillMissingAnchors(analysis);

		return findingMapper.toDtoList(findingRepository.findAllByAnalysis_IdAndAnalysis_User_Id(analysisId, userId));
	}

	@Override
	@Transactional
	public FindingResponseDTO updateFinding(Long analysisId, Long findingId, FindingUpdateRequestDTO request) {
		Long userId = authenticatedUserService.getAuthenticatedUserId();
		if (!analysisRepository.existsByIdAndUser_Id(analysisId, userId)) {
			throw new ResourceNotFoundException("Analysis not found: id=" + analysisId);
		}

		if (request == null) {
			throw new IllegalArgumentException("Request body is required");
		}

		Finding finding = findingRepository.findByIdAndAnalysis_IdAndAnalysis_User_Id(findingId, analysisId, userId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Finding not found: id=" + findingId + " for analysis id=" + analysisId));

		if (request.getProfessorNotes() != null) {
			finding.setProfessorNotes(request.getProfessorNotes());
		}
		if (request.getReviewed() != null) {
			finding.setReviewed(request.getReviewed());
		}
		if (request.getFlaggedForFollowUp() != null) {
			finding.setFlaggedForFollowUp(request.getFlaggedForFollowUp());
		}

		Finding saved = findingRepository.save(finding);
		return findingMapper.toDto(saved);
	}
}