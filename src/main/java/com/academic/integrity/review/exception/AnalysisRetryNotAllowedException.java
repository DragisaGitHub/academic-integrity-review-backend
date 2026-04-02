package com.academic.integrity.review.exception;

public class AnalysisRetryNotAllowedException extends RuntimeException {

	public AnalysisRetryNotAllowedException(String message) {
		super(message);
	}
}