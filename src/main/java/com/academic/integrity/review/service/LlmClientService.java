package com.academic.integrity.review.service;

public interface LlmClientService {

	LlmAnalysisResult analyze(String prompt);

	public record LlmAnalysisResult(String content, String model, Integer totalTokens) {
	}
}