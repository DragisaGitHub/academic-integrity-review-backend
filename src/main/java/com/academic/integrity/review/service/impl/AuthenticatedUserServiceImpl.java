package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.domain.User;
import com.academic.integrity.review.exception.ResourceNotFoundException;
import com.academic.integrity.review.repository.UserRepository;
import com.academic.integrity.review.service.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthenticatedUserServiceImpl implements AuthenticatedUserService {

	private final UserRepository userRepository;

	@Override
	public User getAuthenticatedUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !StringUtils.hasText(authentication.getName())) {
			throw new BadCredentialsException("Authentication is required");
		}

		String username = normalizeUsername(authentication.getName());
		return userRepository.findByUsernameIgnoreCase(username)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: username=" + username));
	}

	@Override
	public Long getAuthenticatedUserId() {
		return getAuthenticatedUser().getId();
	}

	private static String normalizeUsername(String username) {
		if (!StringUtils.hasText(username)) {
			throw new IllegalArgumentException("username is required");
		}
		return username.trim().toLowerCase();
	}
}
