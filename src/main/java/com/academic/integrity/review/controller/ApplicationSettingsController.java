package com.academic.integrity.review.controller;

import com.academic.integrity.review.dto.ApplicationSettingsResponseDTO;
import com.academic.integrity.review.dto.ApplicationSettingsUpsertRequestDTO;
import com.academic.integrity.review.service.ApplicationSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class ApplicationSettingsController {

	private final ApplicationSettingsService applicationSettingsService;

	@GetMapping
	public ResponseEntity<ApplicationSettingsResponseDTO> getSettings() {
		return ResponseEntity.ok(applicationSettingsService.getSettings());
	}

	@PostMapping
	public ResponseEntity<ApplicationSettingsResponseDTO> upsertSettings(
			@Valid @RequestBody ApplicationSettingsUpsertRequestDTO request) {
		return ResponseEntity.ok(applicationSettingsService.upsertSettings(request));
	}
}
