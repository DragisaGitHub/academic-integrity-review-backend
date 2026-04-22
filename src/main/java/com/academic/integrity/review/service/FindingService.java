package com.academic.integrity.review.service;

import com.academic.integrity.review.dto.FindingResponseDTO;
import com.academic.integrity.review.dto.FindingUpdateRequestDTO;
import java.util.List;

public interface FindingService {

	List<FindingResponseDTO> getFindingsByAnalysisId(Long analysisId);

	FindingResponseDTO updateFinding(Long analysisId, Long findingId, FindingUpdateRequestDTO request);
}
