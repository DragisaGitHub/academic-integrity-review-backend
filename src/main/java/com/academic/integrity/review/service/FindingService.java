package com.academic.integrity.review.service;

import com.academic.integrity.review.dto.FindingResponseDTO;
import com.academic.integrity.review.exception.ResourceNotFoundException;
import com.academic.integrity.review.mapper.FindingMapper;
import com.academic.integrity.review.repository.AnalysisRepository;
import com.academic.integrity.review.repository.FindingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindingService {

	private final AnalysisRepository analysisRepository;
	private final FindingRepository findingRepository;
	private final FindingMapper findingMapper;

	public List<FindingResponseDTO> getFindingsByAnalysisId(Long analysisId) {
		if (!analysisRepository.existsById(analysisId)) {
			throw new ResourceNotFoundException("Analysis not found: id=" + analysisId);
		}

		return findingMapper.toDtoList(findingRepository.findAllByAnalysis_Id(analysisId));
	}
}
