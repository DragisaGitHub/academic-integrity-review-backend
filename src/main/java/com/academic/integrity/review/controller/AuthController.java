package com.academic.integrity.review.controller;

import com.academic.integrity.review.dto.ChangePasswordRequestDTO;
import com.academic.integrity.review.dto.LoginRequestDTO;
import com.academic.integrity.review.dto.LoginResponseDTO;
import com.academic.integrity.review.dto.UserResponseDTO;
import com.academic.integrity.review.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@GetMapping("/me")
	public ResponseEntity<UserResponseDTO> me() {
		return ResponseEntity.ok(authService.getCurrentUser());
	}

	@PatchMapping("/password")
	public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request) {
		authService.changePassword(request);
		return ResponseEntity.noContent().build();
	}
}