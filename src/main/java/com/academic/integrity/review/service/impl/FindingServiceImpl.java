package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.domain.Finding;
import com.academic.integrity.review.dto.FindingResponseDTO;
import com.academic.integrity.review.dto.FindingUpdateRequestDTO;
import com.academic.integrity.review.exception.ResourceNotFoundException;
import com.academic.integrity.review.mapper.FindingMapper;
import com.academic.integrity.review.repository.AnalysisRepository;
import com.academic.integrity.review.repository.FindingRepository;
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

	@Override
	public List<FindingResponseDTO> getFindingsByAnalysisId(Long analysisId) {
		if (!analysisRepository.existsById(analysisId)) {
			throw new ResourceNotFoundException("Analysis not found: id=" + analysisId);
		}

		return findingMapper.toDtoList(findingRepository.findAllByAnalysis_Id(analysisId));
	}

	@Override
	@Transactional
	public FindingResponseDTO updateFinding(Long analysisId, Long findingId, FindingUpdateRequestDTO request) {
		if (!analysisRepository.existsById(analysisId)) {
			throw new ResourceNotFoundException("Analysis not found: id=" + analysisId);
		}

		if (request == null) {
			throw new IllegalArgumentException("Request body is required");
		}

		Finding finding = findingRepository.findByIdAndAnalysis_Id(findingId, analysisId)
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