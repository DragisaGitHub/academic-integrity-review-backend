package com.academic.integrity.review.repository;

import com.academic.integrity.review.domain.Document;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
	List<Document> findAllByUser_Id(Long userId, Sort sort);

	Optional<Document> findByIdAndUser_Id(Long id, Long userId);
}
