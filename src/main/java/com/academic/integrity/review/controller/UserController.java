package com.academic.integrity.review.controller;

import com.academic.integrity.review.dto.CreateUserRequestDTO;
import com.academic.integrity.review.dto.ResetPasswordRequestDTO;
import com.academic.integrity.review.dto.UpdateUserRequestDTO;
import com.academic.integrity.review.dto.UserResponseDTO;
import com.academic.integrity.review.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping
	public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
		return ResponseEntity.ok(userService.getAllUsers());
	}

	@PostMapping
	public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody CreateUserRequestDTO request) {
		return ResponseEntity.ok(userService.createUser(request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<UserResponseDTO> updateUser(
			@PathVariable Long id,
			@Valid @RequestBody UpdateUserRequestDTO request) {
		return ResponseEntity.ok(userService.updateUser(id, request));
	}

	@PatchMapping("/{id}/password")
	public ResponseEntity<Void> resetPassword(
			@PathVariable Long id,
			@Valid @RequestBody ResetPasswordRequestDTO request) {
		userService.resetPassword(id, request);
		return ResponseEntity.noContent().build();
	}
}