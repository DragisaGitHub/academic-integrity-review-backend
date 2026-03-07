package com.academic.integrity.review.controller;

import com.academic.integrity.review.dto.FindingResponseDTO;
import com.academic.integrity.review.service.FindingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}
