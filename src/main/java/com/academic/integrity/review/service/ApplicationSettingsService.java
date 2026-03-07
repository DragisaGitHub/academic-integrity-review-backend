package com.academic.integrity.review.service;

import com.academic.integrity.review.domain.ApplicationSettings;
import com.academic.integrity.review.domain.ReadingLayout;
import com.academic.integrity.review.dto.ApplicationSettingsResponseDTO;
import com.academic.integrity.review.dto.ApplicationSettingsUpsertRequestDTO;
import com.academic.integrity.review.mapper.ApplicationSettingsMapper;
import com.academic.integrity.review.repository.ApplicationSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationSettingsService {

	private final ApplicationSettingsRepository applicationSettingsRepository;
	private final ApplicationSettingsMapper applicationSettingsMapper;

	@Transactional
	public ApplicationSettingsResponseDTO getSettings() {
		ApplicationSettings settings = applicationSettingsRepository.findTopByOrderByIdAsc()
				.orElseGet(() -> applicationSettingsRepository.save(defaultSettings()));
		return applicationSettingsMapper.toDto(settings);
	}

	@Transactional
	public ApplicationSettingsResponseDTO upsertSettings(ApplicationSettingsUpsertRequestDTO request) {
		ApplicationSettings settings = applicationSettingsRepository.findTopByOrderByIdAsc()
				.orElseGet(ApplicationSettings::new);

		settings.setProfessorName(request.getProfessorName());
		settings.setDepartment(request.getDepartment());
		settings.setUniversity(request.getUniversity());
		settings.setLocalAiEnabled(request.isLocalAiEnabled());
		settings.setDocumentRetentionDays(request.getDocumentRetentionDays());
		settings.setStorageLocation(request.getStorageLocation());
		settings.setLightThemeEnabled(request.isLightThemeEnabled());
		settings.setReadingLayout(request.getReadingLayout());

		ApplicationSettings saved = applicationSettingsRepository.save(settings);
		return applicationSettingsMapper.toDto(saved);
	}

	private static ApplicationSettings defaultSettings() {
		ApplicationSettings settings = new ApplicationSettings();
		settings.setProfessorName("");
		settings.setDepartment("");
		settings.setUniversity("");
		settings.setLocalAiEnabled(false);
		settings.setDocumentRetentionDays(30);
		settings.setStorageLocation("");
		settings.setLightThemeEnabled(false);
		settings.setReadingLayout(ReadingLayout.COMFORTABLE);
		return settings;
	}
}
