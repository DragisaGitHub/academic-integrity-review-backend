package com.academic.integrity.review.service;

public interface PromptTemplateService {

	String buildPrompt(String documentText, Long userId);
}
