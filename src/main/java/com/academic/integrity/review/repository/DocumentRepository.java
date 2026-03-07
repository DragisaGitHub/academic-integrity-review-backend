package com.academic.integrity.review.repository;

import com.academic.integrity.review.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}
