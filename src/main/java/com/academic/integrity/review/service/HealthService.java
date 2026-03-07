package com.academic.integrity.review.service;

import com.academic.integrity.review.dto.HealthResponseDTO;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

	public HealthResponseDTO getHealth() {
		return new HealthResponseDTO("UP");
	}
}
