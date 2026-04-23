package com.academic.integrity.review.service;

public interface LlmClientService {

	LlmAnalysisResult analyze(String prompt);

	public record LlmAnalysisResult(
			String content,
			String model,
			Integer totalTokens,
			String finishReason,
			String responseId) {

		public LlmAnalysisResult(String content, String model, Integer totalTokens) {
			this(content, model, totalTokens, null, null);
		}
	}
}