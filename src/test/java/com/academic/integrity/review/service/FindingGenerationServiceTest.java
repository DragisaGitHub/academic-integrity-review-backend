package com.academic.integrity.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.Finding;
import com.academic.integrity.review.domain.FindingCategory;
import com.academic.integrity.review.domain.FindingSeverity;
import com.academic.integrity.review.exception.AiFindingsResponseException;
import com.academic.integrity.review.repository.FindingRepository;
import com.academic.integrity.review.service.FindingAnchorService;
import com.academic.integrity.review.service.impl.FindingGenerationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindingGenerationServiceTest {

	@Mock
	private FindingRepository findingRepository;

	@Mock
	private FindingAnchorService findingAnchorService;

	private FindingGenerationService findingGenerationService;

	@BeforeEach
	void setUp() {
		findingGenerationService = new FindingGenerationServiceImpl(
				findingRepository,
				findingAnchorService,
				new ObjectMapper());
	}

	@Test
	void generateFindingsNormalizesAndSkipsInvalidItems() {
		Analysis analysis = new Analysis();
		analysis.setId(99L);

		String rawJson = """
				{
				  "findings": [
				    {
				      "category": "citation_issue",
				      "severity": "high",
				      "title": "Unsupported claim",
				      "explanation": "A claim lacks support.",
				      "excerpt": "This statement has no citation.",
				      "paragraphLocation": "Paragraph 2",
				      "suggestedAction": "Add a citation."
				    },
				    {
				      "category": "unknown category",
				      "severity": "unknown severity",
				      "title": "Fallback example"
				    },
				    {
				      "category": "PLAGIARISM",
				      "severity": "CRITICAL",
				      "title": "   "
				    }
				  ]
				}
				""";

		when(findingRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

		int created = findingGenerationService.generateFindings(analysis, rawJson);

		assertThat(created).isEqualTo(2);

		ArgumentCaptor<List<Finding>> captor = ArgumentCaptor.forClass(List.class);
		verify(findingRepository).deleteByAnalysis_Id(99L);
		verify(findingRepository).saveAll(captor.capture());

		List<Finding> findings = captor.getValue();
		verify(findingAnchorService).assignAnchors(analysis, findings);
		assertThat(findings).hasSize(2);
		assertThat(findings.get(0).getAnalysis()).isSameAs(analysis);
		assertThat(findings.get(0).getCategory()).isEqualTo(FindingCategory.CITATION_ISSUE);
		assertThat(findings.get(0).getSeverity()).isEqualTo(FindingSeverity.HIGH);
		assertThat(findings.get(1).getCategory()).isEqualTo(FindingCategory.OTHER);
		assertThat(findings.get(1).getSeverity()).isEqualTo(FindingSeverity.MEDIUM);
	}

	@Test
	void generateFindingsRecoversWhenJsonIsWrappedInExtraText() {
		Analysis analysis = new Analysis();
		analysis.setId(100L);

		String rawJson = """
				Here is the requested payload:
				```json
				{"findings":[{"category":"CITATION_ISSUE","severity":"HIGH","title":"Missing citation","explanation":"A factual claim has no source.","excerpt":"A key claim without citation.","paragraphLocation":"Paragraph 1","suggestedAction":"Add a supporting citation."}]}
				```
				""";

		when(findingRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

		int created = findingGenerationService.generateFindings(analysis, rawJson);

		assertThat(created).isEqualTo(1);
		verify(findingRepository).deleteByAnalysis_Id(100L);
		verify(findingRepository).saveAll(anyList());
		verify(findingAnchorService).assignAnchors(analysis, anyList());
	}

	@Test
	void generateFindingsThrowsTypedExceptionForMalformedJson() {
		Analysis analysis = new Analysis();
		analysis.setId(101L);

		String rawJson = """
				{"findings":[{"category":"CITATION_ISSUE","severity":"HIGH","title":"Broken","explanation":"The response stops early
				""";

		assertThatThrownBy(() -> findingGenerationService.generateFindings(analysis, rawJson))
				.isInstanceOf(AiFindingsResponseException.class)
				.hasMessageContaining("Malformed AI findings JSON")
				.extracting(ex -> ((AiFindingsResponseException) ex).getPublicMessage())
				.isEqualTo("AI returned malformed findings output. Please retry the analysis.");

		verify(findingRepository, never()).deleteByAnalysis_Id(101L);
		verify(findingRepository, never()).saveAll(anyList());
		verify(findingAnchorService, never()).assignAnchors(analysis, anyList());
	}
}