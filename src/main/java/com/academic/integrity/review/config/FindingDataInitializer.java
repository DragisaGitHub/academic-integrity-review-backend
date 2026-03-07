package com.academic.integrity.review.config;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.Finding;
import com.academic.integrity.review.domain.FindingCategory;
import com.academic.integrity.review.domain.FindingSeverity;
import com.academic.integrity.review.repository.AnalysisRepository;
import com.academic.integrity.review.repository.FindingRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(3)
public class FindingDataInitializer implements CommandLineRunner {

	private final FindingRepository findingRepository;
	private final AnalysisRepository analysisRepository;

	@Override
	public void run(String... args) {
		if (findingRepository.count() > 0) {
			return;
		}

		List<Analysis> analyses = analysisRepository.findAll();
		if (analyses.isEmpty()) {
			return;
		}

		List<Finding> findings = new ArrayList<>();
		for (Analysis analysis : analyses) {
			Finding finding1 = getFinding(analysis);

			Finding finding2 = new Finding();
			finding2.setAnalysis(analysis);
			finding2.setCategory(FindingCategory.CITATION_ISSUE);
			finding2.setSeverity(FindingSeverity.LOW);
			finding2.setTitle("Missing citation");
			finding2.setExplanation("A claim appears unsupported by a citation.");
			finding2.setExcerpt("... example excerpt ...");
			finding2.setParagraphLocation("P4");
			finding2.setSuggestedAction("Request proper citation or supporting evidence.");

			findings.add(finding1);
			findings.add(finding2);
		}

		findingRepository.saveAll(findings);
	}

	private static @NonNull Finding getFinding(Analysis analysis) {
		Finding finding1 = new Finding();
		finding1.setAnalysis(analysis);
		finding1.setCategory(FindingCategory.AI_GENERATED_CONTENT);
		finding1.setSeverity(FindingSeverity.MEDIUM);
		finding1.setTitle("Possible AI-generated phrasing");
		finding1.setExplanation("This section shows patterns consistent with AI-assisted generation.");
		finding1.setExcerpt("... example excerpt ...");
		finding1.setParagraphLocation("P2");
		finding1.setSuggestedAction("Ask the student to provide drafts and sources.");
		return finding1;
	}
}
