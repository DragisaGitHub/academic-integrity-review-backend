package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.domain.User;
import com.academic.integrity.review.dto.ChangePasswordRequestDTO;
import com.academic.integrity.review.dto.LoginRequestDTO;
import com.academic.integrity.review.dto.LoginResponseDTO;
import com.academic.integrity.review.dto.UserResponseDTO;
import com.academic.integrity.review.exception.ResourceNotFoundException;
import com.academic.integrity.review.mapper.UserMapper;
import com.academic.integrity.review.repository.UserRepository;
import com.academic.integrity.review.service.AuthenticatedUserService;
import com.academic.integrity.review.service.AuthService;
import com.academic.integrity.review.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final JwtService jwtService;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticatedUserService authenticatedUserService;

	@Override
	@Transactional(readOnly = true)
	public LoginResponseDTO login(LoginRequestDTO request) {
		if (request == null) {
			throw new IllegalArgumentException("Request body is required");
		}

		String username = normalizeUsername(request.getUsername());
		String password = request.getPassword();

		authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

		User user = userRepository.findByUsernameIgnoreCase(username)
				.filter(User::isEnabled)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: username=" + username));

		UserResponseDTO userDto = userMapper.toDto(user);
		return new LoginResponseDTO("Bearer", jwtService.generateToken(user), userDto);
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponseDTO getCurrentUser() {
		return userMapper.toDto(authenticatedUserService.getAuthenticatedUser());
	}

	@Override
	@Transactional
	public void changePassword(ChangePasswordRequestDTO request) {
		if (request == null) {
			throw new IllegalArgumentException("Request body is required");
		}

		User user = authenticatedUserService.getAuthenticatedUser();
		if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
			throw new BadCredentialsException("Invalid current password");
		}

		user.setPasswordHash(passwordEncoder.encode(request.getNewPassword().trim()));
		userRepository.save(user);
	}

	private static String normalizeUsername(String username) {
		if (!StringUtils.hasText(username)) {
			throw new IllegalArgumentException("username is required");
		}
		return username.trim().toLowerCase();
	}
}