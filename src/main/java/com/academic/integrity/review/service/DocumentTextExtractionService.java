package com.academic.integrity.review.service;

import com.academic.integrity.review.domain.Document;
public interface DocumentTextExtractionService {

	String extractText(Document document);
}
