package com.academic.integrity.review.config;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.Document;
import com.academic.integrity.review.repository.AnalysisRepository;
import com.academic.integrity.review.repository.DocumentRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(2)
public class AnalysisDataInitializer implements CommandLineRunner {

	private final AnalysisRepository analysisRepository;
	private final DocumentRepository documentRepository;

	@Override
	public void run(String... args) {
		if (analysisRepository.count() > 0) {
			return;
		}

		List<Document> documents = documentRepository.findAll();
		if (documents.isEmpty()) {
			return;
		}

		List<Analysis> analyses = new ArrayList<>();
		for (Document document : documents) {
			Analysis analysis = new Analysis();
			analysis.setDocument(document);
			analysis.setAnalysisDate(LocalDate.now());
			analysis.setFullText("Demo analysis for document '" + document.getTitle() + "'.");
			analyses.add(analysis);
		}

		analysisRepository.saveAll(analyses);
	}
}
