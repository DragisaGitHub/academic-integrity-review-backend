package com.academic.integrity.review.repository;

import com.academic.integrity.review.domain.User;
import com.academic.integrity.review.domain.UserRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsernameIgnoreCase(String username);

	boolean existsByRoleAndEnabledTrue(UserRole role);

	long countByRoleAndEnabledTrue(UserRole role);
}