package com.academic.integrity.review.service;

import com.academic.integrity.review.dto.FindingResponseDTO;
import java.util.List;
public interface FindingService {

	List<FindingResponseDTO> getFindingsByAnalysisId(Long analysisId);
}
