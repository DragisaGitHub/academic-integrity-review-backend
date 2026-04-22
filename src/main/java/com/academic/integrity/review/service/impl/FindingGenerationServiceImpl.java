package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.Finding;
import com.academic.integrity.review.domain.FindingCategory;
import com.academic.integrity.review.domain.FindingSeverity;
import com.academic.integrity.review.repository.FindingRepository;
import com.academic.integrity.review.service.FindingGenerationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FindingGenerationServiceImpl implements FindingGenerationService {

	private final FindingRepository findingRepository;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional
	public int generateFindings(Analysis analysis, String rawJson) {
		LlmAnalysisResponse response = parseResponse(rawJson);
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

		findingRepository.saveAll(findings);
		return findings.size();
	}

	private LlmAnalysisResponse parseResponse(String rawJson) {
		try {
			return configuredObjectMapper().readValue(rawJson, LlmAnalysisResponse.class);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("Failed to parse AI findings response", ex);
		}
	}

	private ObjectMapper configuredObjectMapper() {
		return objectMapper.copy()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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