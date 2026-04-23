package com.academic.integrity.review.repository;

import com.academic.integrity.review.domain.Analysis;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
	List<Analysis> findAllByUser_Id(Long userId, Sort sort);

	Optional<Analysis> findByDocument_IdAndUser_Id(Long documentId, Long userId);

	Optional<Analysis> findByIdAndUser_Id(Long id, Long userId);

	@EntityGraph(attributePaths = "document")
	Optional<Analysis> findWithDocumentByIdAndUser_Id(Long id, Long userId);

	List<Analysis> findAllByDocument_IdInAndUser_Id(Collection<Long> documentIds, Long userId);

	boolean existsByIdAndUser_Id(Long id, Long userId);
}
