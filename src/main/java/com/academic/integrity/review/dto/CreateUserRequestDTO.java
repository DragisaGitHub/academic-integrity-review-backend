package com.academic.integrity.review.dto;

import com.academic.integrity.review.domain.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequestDTO {

	@NotBlank(message = "username is required")
	private String username;

	@NotBlank(message = "displayName is required")
	private String displayName;

	@NotBlank(message = "password is required")
	private String password;

	@NotNull(message = "role is required")
	private UserRole role;

	private boolean enabled = true;
}