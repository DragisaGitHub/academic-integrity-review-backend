package com.academic.integrity.review.service;

import com.academic.integrity.review.domain.Analysis;
public interface FindingGenerationService {

	int generateFindings(Analysis analysis, String rawJson);
}