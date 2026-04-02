package com.academic.integrity.review.service;

import com.academic.integrity.review.dto.ApplicationSettingsResponseDTO;
import com.academic.integrity.review.dto.ApplicationSettingsUpsertRequestDTO;
public interface ApplicationSettingsService {

	ApplicationSettingsResponseDTO getSettings();

	ApplicationSettingsResponseDTO upsertSettings(ApplicationSettingsUpsertRequestDTO request);
}
