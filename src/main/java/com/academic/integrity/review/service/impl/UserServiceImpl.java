package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.domain.User;
import com.academic.integrity.review.domain.UserRole;
import com.academic.integrity.review.dto.CreateUserRequestDTO;
import com.academic.integrity.review.dto.ResetPasswordRequestDTO;
import com.academic.integrity.review.dto.UpdateUserRequestDTO;
import com.academic.integrity.review.dto.UserResponseDTO;
import com.academic.integrity.review.exception.ResourceNotFoundException;
import com.academic.integrity.review.mapper.UserMapper;
import com.academic.integrity.review.repository.UserRepository;
import com.academic.integrity.review.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional(readOnly = true)
	public List<UserResponseDTO> getAllUsers() {
		return userMapper.toDtoList(userRepository.findAll(Sort.by(Sort.Direction.ASC, "username")));
	}

	@Override
	@Transactional
	public UserResponseDTO createUser(CreateUserRequestDTO request) {
		if (request == null) {
			throw new IllegalArgumentException("Request body is required");
		}

		String username = normalizeUsername(request.getUsername());
		if (userRepository.findByUsernameIgnoreCase(username).isPresent()) {
			throw new IllegalArgumentException("Username already exists");
		}

		User user = new User();
		user.setUsername(username);
		user.setDisplayName(normalizeRequired(request.getDisplayName(), "displayName"));
		user.setPasswordHash(passwordEncoder.encode(normalizeRequired(request.getPassword(), "password")));
		user.setRole(request.getRole());
		user.setEnabled(request.isEnabled());

		return userMapper.toDto(userRepository.save(user));
	}

	@Override
	@Transactional
	public UserResponseDTO updateUser(Long id, UpdateUserRequestDTO request) {
		if (request == null) {
			throw new IllegalArgumentException("Request body is required");
		}

		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: id=" + id));

		validateAdminRetention(user, request.getRole(), request.isEnabled());

		user.setDisplayName(normalizeRequired(request.getDisplayName(), "displayName"));
		user.setRole(request.getRole());
		user.setEnabled(request.isEnabled());

		return userMapper.toDto(userRepository.save(user));
	}

	@Override
	@Transactional
	public void resetPassword(Long id, ResetPasswordRequestDTO request) {
		if (request == null) {
			throw new IllegalArgumentException("Request body is required");
		}

		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: id=" + id));
		user.setPasswordHash(passwordEncoder.encode(normalizeRequired(request.getNewPassword(), "newPassword")));
		userRepository.save(user);
	}

	private void validateAdminRetention(User existingUser, UserRole newRole, boolean enabled) {
		boolean existingEnabledAdmin = existingUser.getRole() == UserRole.ADMIN && existingUser.isEnabled();
		boolean remainsEnabledAdmin = newRole == UserRole.ADMIN && enabled;

		if (!existingEnabledAdmin || remainsEnabledAdmin) {
			return;
		}

		if (userRepository.countByRoleAndEnabledTrue(UserRole.ADMIN) <= 1) {
			throw new IllegalArgumentException("At least one enabled admin user is required");
		}
	}

	private static String normalizeUsername(String username) {
		return normalizeRequired(username, "username").toLowerCase();
	}

	private static String normalizeRequired(String value, String fieldName) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(fieldName + " is required");
		}
		return value.trim();
	}
}