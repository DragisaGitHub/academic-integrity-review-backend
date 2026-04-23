package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.domain.ApplicationSettings;
import com.academic.integrity.review.domain.ColorTheme;
import com.academic.integrity.review.domain.DisplayDensity;
import com.academic.integrity.review.domain.ReadingLayout;
import com.academic.integrity.review.domain.User;
import com.academic.integrity.review.dto.ApplicationSettingsResponseDTO;
import com.academic.integrity.review.dto.ApplicationSettingsUpsertRequestDTO;
import com.academic.integrity.review.mapper.ApplicationSettingsMapper;
import com.academic.integrity.review.repository.ApplicationSettingsRepository;
import com.academic.integrity.review.service.ApplicationSettingsService;
import com.academic.integrity.review.service.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationSettingsServiceImpl implements ApplicationSettingsService {

	private final ApplicationSettingsRepository applicationSettingsRepository;
	private final ApplicationSettingsMapper applicationSettingsMapper;
	private final AuthenticatedUserService authenticatedUserService;

	@Override
	@Transactional
	public ApplicationSettingsResponseDTO getSettings() {
		User user = authenticatedUserService.getAuthenticatedUser();
		ApplicationSettings settings = applicationSettingsRepository.findByUserId(user.getId())
				.orElseGet(() -> applicationSettingsRepository.save(defaultSettings(user)));
		return applicationSettingsMapper.toDto(settings);
	}

	@Override
	@Transactional
	public ApplicationSettingsResponseDTO upsertSettings(ApplicationSettingsUpsertRequestDTO request) {
		if (request == null) {
			throw new IllegalArgumentException("Request body is required");
		}

		User user = authenticatedUserService.getAuthenticatedUser();
		ApplicationSettings settings = applicationSettingsRepository.findByUserId(user.getId())
				.orElseGet(() -> defaultSettings(user));

		settings.setProfessorName(request.getProfessorName());
		settings.setDepartment(request.getDepartment());
		settings.setUniversity(request.getUniversity());
		settings.setEmail(request.getEmail() != null ? request.getEmail() : "");

		settings.setCitationAnalysis(request.isCitationAnalysis());
		settings.setReferenceValidation(request.isReferenceValidation());
		settings.setFactualConsistencyReview(request.isFactualConsistencyReview());
		settings.setWritingStyleConsistency(request.isWritingStyleConsistency());
		settings.setAiReviewAssistance(request.isAiReviewAssistance());

		settings.setLocalAiEnabled(request.isLocalAiEnabled());
		settings.setDocumentRetentionDays(request.getDocumentRetentionDays());
		settings.setAutoDeleteReviewedDocuments(request.isAutoDeleteReviewedDocuments());
		settings.setStorageLocation(request.getStorageLocation());

		ColorTheme colorTheme = request.getColorTheme();
		if (colorTheme != null) {
			settings.setColorTheme(colorTheme);
			settings.setLightThemeEnabled(colorTheme == ColorTheme.LIGHT);
		}

		DisplayDensity displayDensity = request.getDisplayDensity();
		if (displayDensity != null) {
			settings.setDisplayDensity(displayDensity);
		}

		settings.setShowSeverityBadges(request.isShowSeverityBadges());
		settings.setReadingLayout(request.getReadingLayout());

		ApplicationSettings saved = applicationSettingsRepository.save(settings);
		return applicationSettingsMapper.toDto(saved);
	}

	private static ApplicationSettings defaultSettings(User user) {
		ApplicationSettings settings = new ApplicationSettings();
		settings.setUser(user);
		settings.setProfessorName("");
		settings.setDepartment("");
		settings.setUniversity("");
		settings.setEmail("");

		settings.setCitationAnalysis(false);
		settings.setReferenceValidation(false);
		settings.setFactualConsistencyReview(false);
		settings.setWritingStyleConsistency(false);
		settings.setAiReviewAssistance(false);

		settings.setLocalAiEnabled(false);
		settings.setDocumentRetentionDays(30);
		settings.setAutoDeleteReviewedDocuments(false);
		settings.setStorageLocation("");

		settings.setColorTheme(ColorTheme.DARK);
		settings.setLightThemeEnabled(false);
		settings.setDisplayDensity(DisplayDensity.COMFORTABLE);
		settings.setShowSeverityBadges(true);
		settings.setReadingLayout(ReadingLayout.DEFAULT);
		return settings;
	}
}
