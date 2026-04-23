package com.academic.integrity.review.service;

import com.academic.integrity.review.domain.User;

public interface AuthenticatedUserService {

	User getAuthenticatedUser();

	Long getAuthenticatedUserId();
}
