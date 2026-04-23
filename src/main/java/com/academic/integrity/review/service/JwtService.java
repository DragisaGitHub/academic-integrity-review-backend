package com.academic.integrity.review.service;

import com.academic.integrity.review.domain.User;
import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {

	String generateToken(User user);

	String extractUsername(String token);

	boolean isTokenValid(String token, UserDetails userDetails);
}