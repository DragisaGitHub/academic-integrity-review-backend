package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.AnalysisStatus;
import com.academic.integrity.review.domain.Document;
import com.academic.integrity.review.exception.ResourceNotFoundException;
import com.academic.integrity.review.repository.AnalysisRepository;
import com.academic.integrity.review.service.AnalysisOrchestrationService;
import com.academic.integrity.review.service.DocumentTextExtractionService;
import com.academic.integrity.review.service.FindingGenerationService;
import com.academic.integrity.review.service.LlmClientService;
import com.academic.integrity.review.service.NotificationService;
import com.academic.integrity.review.service.PromptTemplateService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalysisOrchestrationServiceImpl implements AnalysisOrchestrationService {

	private static final Logger log = LoggerFactory.getLogger(AnalysisOrchestrationServiceImpl.class);

	private final AnalysisRepository analysisRepository;
	private final DocumentTextExtractionService documentTextExtractionService;
	private final PromptTemplateService promptTemplateService;
	private final LlmClientService llmClientService;
	private final FindingGenerationService findingGenerationService;
	private final NotificationService notificationService;

	@Override
	@Async("analysisTaskExecutor")
	public void runAnalysis(Long analysisId) {
		Analysis analysis = analysisRepository.findWithDocumentById(analysisId)
				.orElseThrow(() -> new ResourceNotFoundException("Analysis not found: id=" + analysisId));

		try {
			analysis.setStartedAt(Instant.now());
			updateAnalysis(analysis, AnalysisStatus.EXTRACTING, null);

			Document document = analysis.getDocument();
			String fullText = documentTextExtractionService.extractText(document);
			if (fullText == null || fullText.isBlank()) {
				throw new IllegalArgumentException("Extracted document text is empty");
			}

			analysis.setFullText(fullText);
			updateAnalysis(analysis, AnalysisStatus.ANALYZING, null);

			String prompt = promptTemplateService.buildPrompt(fullText);
			LlmClientService.LlmAnalysisResult result = llmClientService.analyze(prompt);
			findingGenerationService.generateFindings(analysis, result.content());

			analysis.setModelName(result.model());
			analysis.setTotalTokensUsed(result.totalTokens());
			analysis.setCompletedAt(Instant.now());
			updateAnalysis(analysis, AnalysisStatus.COMPLETED, null);
			notificationService.createAnalysisCompletedNotification(analysis);
			log.info("Analysis {} completed", analysisId);
		} catch (Exception ex) {
			log.error("Analysis {} failed", analysisId, ex);
			analysis.setCompletedAt(Instant.now());
			String errorMessage = failureMessage(ex);
			updateAnalysis(analysis, AnalysisStatus.FAILED, errorMessage);
			notificationService.createAnalysisFailedNotification(analysis, errorMessage);
		}
	}

	private void updateAnalysis(Analysis analysis, AnalysisStatus status, String errorMessage) {
		analysis.setAnalysisStatus(status);
		analysis.setErrorMessage(errorMessage);
		analysisRepository.saveAndFlush(analysis);
	}

	private static String failureMessage(Exception ex) {
		String message = ex.getMessage();
		return (message == null || message.isBlank()) ? "Analysis failed" : message;
	}
}