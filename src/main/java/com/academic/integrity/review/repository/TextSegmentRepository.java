package com.academic.integrity.review.repository;

import com.academic.integrity.review.domain.TextSegment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TextSegmentRepository extends JpaRepository<TextSegment, Long> {

	long countByAnalysis_Id(Long analysisId);

	void deleteByAnalysis_Id(Long analysisId);

	List<TextSegment> findAllByAnalysis_IdOrderBySegmentIndexAsc(Long analysisId);
}