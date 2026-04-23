package com.academic.integrity.review.repository;

import com.academic.integrity.review.domain.ApplicationSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationSettingsRepository extends JpaRepository<ApplicationSettings, Long> {

	@Query("""
			select settings
			from ApplicationSettings settings
			where settings.user.id = :userId
			""")
	Optional<ApplicationSettings> findByUserId(@Param("userId") Long userId);
}

