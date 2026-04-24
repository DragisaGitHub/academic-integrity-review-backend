package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.TextSegment;
import com.academic.integrity.review.dto.TextSegmentDTO;
import com.academic.integrity.review.exception.ResourceNotFoundException;
import com.academic.integrity.review.mapper.TextSegmentMapper;
import com.academic.integrity.review.repository.AnalysisRepository;
import com.academic.integrity.review.repository.TextSegmentRepository;
import com.academic.integrity.review.service.AuthenticatedUserService;
import com.academic.integrity.review.service.TextSegmentService;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TextSegmentServiceImpl implements TextSegmentService {

	private static final Pattern PARAGRAPH_DELIMITER = Pattern.compile("(?:\\R){2,}");
	private static final int MIN_SEGMENT_CHARACTERS = 50;

	private final AnalysisRepository analysisRepository;
	private final TextSegmentRepository textSegmentRepository;
	private final TextSegmentMapper textSegmentMapper;
	private final AuthenticatedUserService authenticatedUserService;

	@Override
	@Transactional
	public void replaceSegments(Analysis analysis, String fullText) {
		if (analysis == null || analysis.getId() == null) {
			throw new IllegalArgumentException("Analysis is required");
		}
		textSegmentRepository.deleteByAnalysis_Id(analysis.getId());
		List<TextSegment> segments = buildSegments(analysis, fullText);
		if (!segments.isEmpty()) {
			textSegmentRepository.saveAll(segments);
		}
	}

	@Override
	@Transactional
	public List<TextSegment> ensureTextSegments(Analysis analysis) {
		if (analysis == null || analysis.getId() == null) {
			throw new IllegalArgumentException("Analysis is required");
		}
		if (textSegmentRepository.countByAnalysis_Id(analysis.getId()) == 0L && StringUtils.hasText(analysis.getFullText())) {
			replaceSegments(analysis, analysis.getFullText());
		}
		return textSegmentRepository.findAllByAnalysis_IdOrderBySegmentIndexAsc(analysis.getId());
	}

	@Override
	@Transactional
	public List<TextSegmentDTO> getAnalysisTextSegments(Long analysisId, Integer from, Integer to) {
		validateRange(from, to);
		Long userId = authenticatedUserService.getAuthenticatedUserId();
		Analysis analysis = analysisRepository.findByIdAndUser_Id(analysisId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Analysis not found: id=" + analysisId));
		List<TextSegment> segments = ensureTextSegments(analysis).stream()
				.filter(segment -> from == null || segment.getSegmentIndex() >= from)
				.filter(segment -> to == null || segment.getSegmentIndex() <= to)
				.toList();
		return textSegmentMapper.toDtoList(segments);
	}

	private static void validateRange(Integer from, Integer to) {
		if (from != null && from < 0) {
			throw new IllegalArgumentException("from must be greater than or equal to 0");
		}
		if (to != null && to < 0) {
			throw new IllegalArgumentException("to must be greater than or equal to 0");
		}
		if (from != null && to != null && to < from) {
			throw new IllegalArgumentException("to must be greater than or equal to from");
		}
	}

	private static List<TextSegment> buildSegments(Analysis analysis, String fullText) {
		if (!StringUtils.hasText(fullText)) {
			return List.of();
		}

		List<SegmentWindow> windows = new ArrayList<>();
		Matcher matcher = PARAGRAPH_DELIMITER.matcher(fullText);
		int cursor = 0;
		while (matcher.find()) {
			addWindow(windows, fullText, cursor, matcher.start());
			cursor = matcher.end();
		}
		addWindow(windows, fullText, cursor, fullText.length());

		List<SegmentWindow> merged = mergeSmallWindows(windows);
		List<TextSegment> segments = new ArrayList<>(merged.size());
		for (int index = 0; index < merged.size(); index++) {
			SegmentWindow window = merged.get(index);
			TextSegment segment = new TextSegment();
			segment.setAnalysis(analysis);
			segment.setSegmentIndex(index);
			segment.setContent(fullText.substring(window.startOffset(), window.endOffset()));
			segment.setStartOffset(window.startOffset());
			segment.setEndOffset(window.endOffset());
			segments.add(segment);
		}
		return segments;
	}

	private static void addWindow(List<SegmentWindow> windows, String fullText, int rawStart, int rawEnd) {
		int start = rawStart;
		int end = rawEnd;
		while (start < end && Character.isWhitespace(fullText.charAt(start))) {
			start++;
		}
		while (end > start && Character.isWhitespace(fullText.charAt(end - 1))) {
			end--;
		}
		if (start < end) {
			windows.add(new SegmentWindow(start, end));
		}
	}

	private static List<SegmentWindow> mergeSmallWindows(List<SegmentWindow> windows) {
		if (windows.isEmpty()) {
			return windows;
		}
		List<SegmentWindow> merged = new ArrayList<>();
		for (SegmentWindow current : windows) {
			if (merged.isEmpty()) {
				merged.add(current);
				continue;
			}

			SegmentWindow previous = merged.get(merged.size() - 1);
			int currentLength = current.endOffset() - current.startOffset();
			int previousLength = previous.endOffset() - previous.startOffset();
			if (currentLength < MIN_SEGMENT_CHARACTERS || previousLength < MIN_SEGMENT_CHARACTERS) {
				merged.set(merged.size() - 1, new SegmentWindow(previous.startOffset(), current.endOffset()));
				continue;
			}

			merged.add(current);
		}
		return merged;
	}

	private record SegmentWindow(int startOffset, int endOffset) {
	}
}