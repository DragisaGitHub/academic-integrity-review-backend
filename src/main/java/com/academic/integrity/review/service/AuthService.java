package com.academic.integrity.review.service;

import com.academic.integrity.review.dto.ChangePasswordRequestDTO;
import com.academic.integrity.review.dto.LoginRequestDTO;
import com.academic.integrity.review.dto.LoginResponseDTO;
import com.academic.integrity.review.dto.UserResponseDTO;

public interface AuthService {

	LoginResponseDTO login(LoginRequestDTO request);

	UserResponseDTO getCurrentUser();

	void changePassword(ChangePasswordRequestDTO request);
}