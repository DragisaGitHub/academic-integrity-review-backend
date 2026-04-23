package com.academic.integrity.review.exception;

import org.springframework.util.StringUtils;

public class AiFindingsResponseException extends RuntimeException {

	public enum Kind {
		MALFORMED_JSON,
		INCOMPLETE_RESPONSE,
		REFUSED_RESPONSE,
		EMPTY_RESPONSE
	}

	private final Kind kind;
	private final String publicMessage;

	public AiFindingsResponseException(Kind kind, String publicMessage, String diagnosticMessage) {
		super(diagnosticMessage);
		this.kind = kind;
		this.publicMessage = resolvePublicMessage(publicMessage);
	}

	public AiFindingsResponseException(
			Kind kind,
			String publicMessage,
			String diagnosticMessage,
			Throwable cause) {
		super(diagnosticMessage, cause);
		this.kind = kind;
		this.publicMessage = resolvePublicMessage(publicMessage);
	}

	public Kind getKind() {
		return kind;
	}

	public String getPublicMessage() {
		return publicMessage;
	}

	private static String resolvePublicMessage(String publicMessage) {
		return StringUtils.hasText(publicMessage)
				? publicMessage
				: "AI findings generation failed.";
	}
}