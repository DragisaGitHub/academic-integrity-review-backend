package com.academic.integrity.review.controller;

import com.academic.integrity.review.dto.AnalysisResponseDTO;
import com.academic.integrity.review.dto.CreateAnalysisRequestDTO;
import com.academic.integrity.review.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
public class AnalysisController {

	private final AnalysisService analysisService;

	@GetMapping("/document/{documentId}")
	public AnalysisResponseDTO getAnalysisByDocumentId(@PathVariable Long documentId) {
		return analysisService.getAnalysisByDocumentId(documentId);
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public AnalysisResponseDTO createAnalysis(@RequestBody CreateAnalysisRequestDTO request) {
		return analysisService.createAnalysis(request);
	}
}
