package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.dto.HealthResponseDTO;
import com.academic.integrity.review.service.HealthService;
import org.springframework.stereotype.Service;

@Service
public class HealthServiceImpl implements HealthService {

	@Override
	public HealthResponseDTO getHealth() {
		return new HealthResponseDTO("UP");
	}
}