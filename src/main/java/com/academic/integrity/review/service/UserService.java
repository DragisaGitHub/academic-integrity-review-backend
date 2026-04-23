package com.academic.integrity.review.service;

import com.academic.integrity.review.dto.CreateUserRequestDTO;
import com.academic.integrity.review.dto.ResetPasswordRequestDTO;
import com.academic.integrity.review.dto.UpdateUserRequestDTO;
import com.academic.integrity.review.dto.UserResponseDTO;
import java.util.List;

public interface UserService {

	List<UserResponseDTO> getAllUsers();

	UserResponseDTO createUser(CreateUserRequestDTO request);

	UserResponseDTO updateUser(Long id, UpdateUserRequestDTO request);

	void resetPassword(Long id, ResetPasswordRequestDTO request);
}