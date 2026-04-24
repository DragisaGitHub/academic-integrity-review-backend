package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.Finding;
import com.academic.integrity.review.domain.TextSegment;
import com.academic.integrity.review.repository.FindingRepository;
import com.academic.integrity.review.service.FindingAnchorService;
import com.academic.integrity.review.service.TextSegmentService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FindingAnchorServiceImpl implements FindingAnchorService {

	private final FindingRepository findingRepository;
	private final TextSegmentService textSegmentService;

	@Override
	@Transactional
	public void assignAnchors(Analysis analysis, List<Finding> findings) {
		if (analysis == null || analysis.getId() == null || findings == null || findings.isEmpty()) {
			return;
		}
		String fullText = analysis.getFullText();
		if (!StringUtils.hasText(fullText)) {
			clearAnchors(findings);
			findingRepository.saveAll(findings);
			return;
		}

		List<TextSegment> segments = textSegmentService.ensureTextSegments(analysis);
		IndexedText indexedText = IndexedText.from(fullText);
		List<Finding> updatedFindings = new ArrayList<>();
		for (Finding finding : findings) {
			if (finding == null) {
				continue;
			}
			boolean changed = applyAnchor(finding, indexedText, segments);
			if (changed) {
				updatedFindings.add(finding);
			}
		}
		if (!updatedFindings.isEmpty()) {
			findingRepository.saveAll(updatedFindings);
		}
	}

	@Override
	@Transactional
	public void backfillMissingAnchors(Analysis analysis) {
		if (analysis == null || analysis.getId() == null) {
			return;
		}
		List<Finding> findings = findingRepository.findAllByAnalysis_Id(analysis.getId()).stream()
				.filter(finding -> StringUtils.hasText(finding.getExcerpt()))
				.filter(this::isMissingAnchor)
				.toList();
		assignAnchors(analysis, findings);
	}

	private boolean applyAnchor(Finding finding, IndexedText indexedText, List<TextSegment> segments) {
		Integer originalSegmentIndex = finding.getSegmentIndex();
		Integer originalStartOffset = finding.getExcerptStartOffset();
		Integer originalEndOffset = finding.getExcerptEndOffset();

		TextMatch match = findMatch(indexedText, finding.getExcerpt());
		if (match == null) {
			finding.setSegmentIndex(null);
			finding.setExcerptStartOffset(null);
			finding.setExcerptEndOffset(null);
		} else {
			finding.setExcerptStartOffset(match.startOffset());
			finding.setExcerptEndOffset(match.endOffset());
			finding.setSegmentIndex(resolveSegmentIndex(segments, match.startOffset()));
		}

		return !equalsNullable(originalSegmentIndex, finding.getSegmentIndex())
				|| !equalsNullable(originalStartOffset, finding.getExcerptStartOffset())
				|| !equalsNullable(originalEndOffset, finding.getExcerptEndOffset());
	}

	private static void clearAnchors(List<Finding> findings) {
		for (Finding finding : findings) {
			if (finding == null) {
				continue;
			}
			finding.setSegmentIndex(null);
			finding.setExcerptStartOffset(null);
			finding.setExcerptEndOffset(null);
		}
	}

	private boolean isMissingAnchor(Finding finding) {
		return finding.getSegmentIndex() == null
				|| finding.getExcerptStartOffset() == null
				|| finding.getExcerptEndOffset() == null;
	}

	private static boolean equalsNullable(Integer left, Integer right) {
		return left == null ? right == null : left.equals(right);
	}

	private static Integer resolveSegmentIndex(List<TextSegment> segments, int startOffset) {
		for (TextSegment segment : segments) {
			if (segment.getStartOffset() <= startOffset && startOffset < segment.getEndOffset()) {
				return segment.getSegmentIndex();
			}
		}
		return null;
	}

	private static TextMatch findMatch(IndexedText indexedText, String excerpt) {
		if (!StringUtils.hasText(excerpt)) {
			return null;
		}
		String fullText = indexedText.fullText();
		int exactStart = fullText.indexOf(excerpt);
		if (exactStart >= 0) {
			return new TextMatch(exactStart, exactStart + excerpt.length());
		}

		String normalizedExcerpt = normalizeWhitespace(excerpt);
		if (!StringUtils.hasText(normalizedExcerpt)) {
			return null;
		}

		int normalizedStart = indexedText.normalizedText().indexOf(normalizedExcerpt);
		if (normalizedStart < 0) {
			normalizedStart = indexedText.normalizedLowerText()
					.indexOf(normalizedExcerpt.toLowerCase(Locale.ROOT));
		}
		if (normalizedStart < 0) {
			return null;
		}

		int normalizedEnd = normalizedStart + normalizedExcerpt.length() - 1;
		return new TextMatch(
				indexedText.originalOffsets().get(normalizedStart),
				indexedText.originalOffsets().get(normalizedEnd) + 1);
	}

	private static String normalizeWhitespace(String value) {
		StringBuilder normalized = new StringBuilder();
		boolean previousWhitespace = false;
		for (int index = 0; index < value.length(); index++) {
			char current = value.charAt(index);
			if (Character.isWhitespace(current)) {
				if (!previousWhitespace) {
					normalized.append(' ');
					previousWhitespace = true;
				}
				continue;
			}
			normalized.append(current);
			previousWhitespace = false;
		}
		return normalized.toString().trim();
	}

	private record TextMatch(int startOffset, int endOffset) {
	}

	private record IndexedText(String fullText, String normalizedText, String normalizedLowerText, List<Integer> originalOffsets) {

		private static IndexedText from(String fullText) {
			StringBuilder normalized = new StringBuilder();
			List<Integer> offsets = new ArrayList<>();
			boolean previousWhitespace = false;
			for (int index = 0; index < fullText.length(); index++) {
				char current = fullText.charAt(index);
				if (Character.isWhitespace(current)) {
					if (!previousWhitespace) {
						normalized.append(' ');
						offsets.add(index);
						previousWhitespace = true;
					}
					continue;
				}
				normalized.append(current);
				offsets.add(index);
				previousWhitespace = false;
			}
			String normalizedText = normalized.toString();
			return new IndexedText(fullText, normalizedText, normalizedText.toLowerCase(Locale.ROOT), offsets);
		}
	}
}