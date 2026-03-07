package com.academic.integrity.review.repository;

import com.academic.integrity.review.domain.ReviewNote;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewNoteRepository extends JpaRepository<ReviewNote, Long> {
	Optional<ReviewNote> findByDocument_Id(Long documentId);

	List<ReviewNote> findAllByDocument_IdIn(Collection<Long> documentIds);
}
