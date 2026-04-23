package com.academic.integrity.review.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequestDTO {

	@NotBlank(message = "currentPassword is required")
	private String currentPassword;

	@NotBlank(message = "newPassword is required")
	private String newPassword;
}