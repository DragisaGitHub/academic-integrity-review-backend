package com.academic.integrity.review.service;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.TextSegment;
import com.academic.integrity.review.dto.TextSegmentDTO;
import java.util.List;

public interface TextSegmentService {

	void replaceSegments(Analysis analysis, String fullText);

	List<TextSegment> ensureTextSegments(Analysis analysis);

	List<TextSegmentDTO> getAnalysisTextSegments(Long analysisId, Integer from, Integer to);
}