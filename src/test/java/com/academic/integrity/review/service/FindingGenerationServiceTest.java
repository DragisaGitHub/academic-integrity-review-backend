package com.academic.integrity.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.Finding;
import com.academic.integrity.review.domain.FindingCategory;
import com.academic.integrity.review.domain.FindingSeverity;
import com.academic.integrity.review.repository.FindingRepository;
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

	private FindingGenerationService findingGenerationService;

	@BeforeEach
	void setUp() {
		findingGenerationService = new FindingGenerationServiceImpl(findingRepository, new ObjectMapper());
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
		assertThat(findings).hasSize(2);
		assertThat(findings.get(0).getAnalysis()).isSameAs(analysis);
		assertThat(findings.get(0).getCategory()).isEqualTo(FindingCategory.CITATION_ISSUE);
		assertThat(findings.get(0).getSeverity()).isEqualTo(FindingSeverity.HIGH);
		assertThat(findings.get(1).getCategory()).isEqualTo(FindingCategory.OTHER);
		assertThat(findings.get(1).getSeverity()).isEqualTo(FindingSeverity.MEDIUM);
	}
}