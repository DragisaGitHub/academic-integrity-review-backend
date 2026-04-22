package com.academic.integrity.review.controller;

import com.academic.integrity.review.dto.FindingUpdateRequestDTO;
import com.academic.integrity.review.dto.FindingResponseDTO;
import com.academic.integrity.review.service.FindingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
public class FindingController {

	private final FindingService findingService;

	@GetMapping("/{analysisId}/findings")
	public List<FindingResponseDTO> getFindingsByAnalysisId(@PathVariable Long analysisId) {
		return findingService.getFindingsByAnalysisId(analysisId);
	}

	@PatchMapping("/{analysisId}/findings/{findingId}")
	public FindingResponseDTO updateFinding(
			@PathVariable Long analysisId,
			@PathVariable Long findingId,
			@RequestBody FindingUpdateRequestDTO request) {
		return findingService.updateFinding(analysisId, findingId, request);
	}
}
