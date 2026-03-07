package com.academic.integrity.review.repository;

import com.academic.integrity.review.domain.ApplicationSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationSettingsRepository extends JpaRepository<ApplicationSettings, Long> {
	Optional<ApplicationSettings> findTopByOrderByIdAsc();
}
