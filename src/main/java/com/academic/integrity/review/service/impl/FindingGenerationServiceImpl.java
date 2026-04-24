package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.Finding;
import com.academic.integrity.review.domain.FindingCategory;
import com.academic.integrity.review.domain.FindingSeverity;
import com.academic.integrity.review.exception.AiFindingsResponseException;
import com.academic.integrity.review.repository.FindingRepository;
import com.academic.integrity.review.service.FindingAnchorService;
import com.academic.integrity.review.service.FindingGenerationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FindingGenerationServiceImpl implements FindingGenerationService {

	private static final Logger log = LoggerFactory.getLogger(FindingGenerationServiceImpl.class);

	private final FindingRepository findingRepository;
	private final FindingAnchorService findingAnchorService;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional
	public int generateFindings(Analysis analysis, String rawJson) {
		LlmAnalysisResponse response = parseResponse(analysis.getId(), rawJson);
		findingRepository.deleteByAnalysis_Id(analysis.getId());

		if (response.findings() == null || response.findings().isEmpty()) {
			return 0;
		}

		List<Finding> findings = new ArrayList<>();
		for (LlmFindingItem item : response.findings()) {
			Finding finding = toFinding(analysis, item);
			if (finding != null) {
				findings.add(finding);
			}
		}

		if (findings.isEmpty()) {
			return 0;
		}

		List<Finding> savedFindings = findingRepository.saveAll(findings);
		findingAnchorService.assignAnchors(analysis, savedFindings);
		return findings.size();
	}

	private LlmAnalysisResponse parseResponse(Long analysisId, String rawJson) {
		if (!StringUtils.hasText(rawJson)) {
			throw new AiFindingsResponseException(
					AiFindingsResponseException.Kind.EMPTY_RESPONSE,
					"AI returned an empty findings payload. Please retry the analysis.",
					"AI findings response was blank for analysisId=" + analysisId);
		}

		try {
			return parseCandidate(rawJson);
		} catch (JsonProcessingException ex) {
			String extractedJson = extractJsonPayload(rawJson);
			if (StringUtils.hasText(extractedJson)) {
				try {
					LlmAnalysisResponse recoveredResponse = parseCandidate(extractedJson);
					log.warn(
							"Recovered wrapped AI findings JSON for analysis {}. rawLength={} recoveredLength={}",
							analysisId,
							rawJson.length(),
							extractedJson.length());
					return recoveredResponse;
				} catch (JsonProcessingException recoveryException) {
					ex.addSuppressed(recoveryException);
				}
			}

			log.error(
					"Failed to parse AI findings JSON for analysis {}. error='{}' responseLength={} responseStart='{}' responseEnd='{}'",
					analysisId,
					parserSummary(ex),
					rawJson.length(),
					diagnosticSnippet(rawJson, true),
					diagnosticSnippet(rawJson, false),
					ex);
			throw new AiFindingsResponseException(
					AiFindingsResponseException.Kind.MALFORMED_JSON,
					"AI returned malformed findings output. Please retry the analysis.",
					"Malformed AI findings JSON for analysisId=%s: %s"
							.formatted(analysisId, parserSummary(ex)),
					ex);
		}
	}

	private LlmAnalysisResponse parseCandidate(String rawJson) throws JsonProcessingException {
		return configuredObjectMapper().readValue(rawJson, LlmAnalysisResponse.class);
	}

	private ObjectMapper configuredObjectMapper() {
		return objectMapper.copy()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true);
	}

	private static String extractJsonPayload(String rawJson) {
		String trimmed = rawJson == null ? null : rawJson.trim();
		if (!StringUtils.hasText(trimmed)) {
			return null;
		}

		int firstBrace = trimmed.indexOf('{');
		int lastBrace = trimmed.lastIndexOf('}');
		if (firstBrace < 0 || lastBrace <= firstBrace) {
			return null;
		}
		if (firstBrace == 0 && lastBrace == trimmed.length() - 1) {
			return null;
		}
		return trimmed.substring(firstBrace, lastBrace + 1).trim();
	}

	private static String parserSummary(JsonProcessingException ex) {
		if (ex.getLocation() == null) {
			return ex.getOriginalMessage();
		}
		return "%s at line %d column %d".formatted(
				ex.getOriginalMessage(),
				ex.getLocation().getLineNr(),
				ex.getLocation().getColumnNr());
	}

	private static String diagnosticSnippet(String rawJson, boolean fromStart) {
		if (!StringUtils.hasText(rawJson)) {
			return "";
		}
		String normalized = rawJson
				.replace("\r", "\\r")
				.replace("\n", "\\n")
				.replace("\t", "\\t");
		int snippetLength = Math.min(220, normalized.length());
		String snippet = fromStart
				? normalized.substring(0, snippetLength)
				: normalized.substring(normalized.length() - snippetLength);
		return normalized.length() > snippetLength && !fromStart
				? "..." + snippet
				: snippet;
	}

	private static Finding toFinding(Analysis analysis, LlmFindingItem item) {
		if (item == null || !StringUtils.hasText(item.title())) {
			return null;
		}

		Finding finding = new Finding();
		finding.setAnalysis(analysis);
		finding.setCategory(parseCategory(item.category()));
		finding.setSeverity(parseSeverity(item.severity()));
		finding.setTitle(item.title().trim());
		finding.setExplanation(normalize(item.explanation()));
		finding.setExcerpt(normalize(item.excerpt()));
		finding.setParagraphLocation(normalize(item.paragraphLocation()));
		finding.setSuggestedAction(normalize(item.suggestedAction()));
		return finding;
	}

	private static FindingCategory parseCategory(String raw) {
		String normalized = enumToken(raw);
		if (!StringUtils.hasText(normalized)) {
			return FindingCategory.OTHER;
		}
		try {
			return FindingCategory.valueOf(normalized);
		} catch (IllegalArgumentException ex) {
			return FindingCategory.OTHER;
		}
	}

	private static FindingSeverity parseSeverity(String raw) {
		String normalized = enumToken(raw);
		if (!StringUtils.hasText(normalized)) {
			return FindingSeverity.MEDIUM;
		}
		try {
			return FindingSeverity.valueOf(normalized);
		} catch (IllegalArgumentException ex) {
			return FindingSeverity.MEDIUM;
		}
	}

	private static String enumToken(String raw) {
		String normalized = normalize(raw);
		if (normalized == null) {
			return null;
		}
		return normalized.toUpperCase(Locale.ROOT)
				.replace('-', '_')
				.replace(' ', '_');
	}

	private static String normalize(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private record LlmAnalysisResponse(List<LlmFindingItem> findings) {
	}

	private record LlmFindingItem(
			String category,
			String severity,
			String title,
			String explanation,
			String excerpt,
			String paragraphLocation,
			String suggestedAction) {
	}
}