package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.dto.FindingResponseDTO;
import com.academic.integrity.review.exception.ResourceNotFoundException;
import com.academic.integrity.review.mapper.FindingMapper;
import com.academic.integrity.review.repository.AnalysisRepository;
import com.academic.integrity.review.repository.FindingRepository;
import com.academic.integrity.review.service.FindingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}