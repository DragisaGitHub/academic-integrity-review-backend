package com.academic.integrity.review.repository;

import com.academic.integrity.review.domain.Analysis;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
	Optional<Analysis> findByDocument_Id(Long documentId);

	List<Analysis> findAllByDocument_IdIn(Collection<Long> documentIds);
}
