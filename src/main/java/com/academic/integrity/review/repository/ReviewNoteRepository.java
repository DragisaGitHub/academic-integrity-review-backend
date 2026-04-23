package com.academic.integrity.review.repository;

import com.academic.integrity.review.domain.ReviewNote;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewNoteRepository extends JpaRepository<ReviewNote, Long> {
	Optional<ReviewNote> findByDocument_IdAndUser_Id(Long documentId, Long userId);

	List<ReviewNote> findAllByDocument_IdInAndUser_Id(Collection<Long> documentIds, Long userId);
}
