package com.academic.integrity.review.controller;

import com.academic.integrity.review.dto.HealthResponseDTO;
import com.academic.integrity.review.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

	private final HealthService healthService;

	@GetMapping("/health")
	public ResponseEntity<HealthResponseDTO> health() {
		return ResponseEntity.ok(healthService.getHealth());
	}
}
