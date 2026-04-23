package com.academic.integrity.review.dto;

import com.academic.integrity.review.domain.UserRole;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

	private Long id;
	private String username;
	private String displayName;
	private UserRole role;
	private boolean enabled;
	private Instant createdAt;
	private Instant updatedAt;
}