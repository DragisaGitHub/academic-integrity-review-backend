package com.academic.integrity.review.exception;

public class DocumentDeletionNotAllowedException extends RuntimeException {

	public DocumentDeletionNotAllowedException(String message) {
		super(message);
	}
}