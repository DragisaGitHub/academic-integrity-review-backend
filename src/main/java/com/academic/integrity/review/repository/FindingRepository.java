package com.academic.integrity.review.repository;

import com.academic.integrity.review.domain.Finding;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FindingRepository extends JpaRepository<Finding, Long> {
	void deleteByAnalysis_Id(Long analysisId);

	List<Finding> findAllByAnalysis_Id(Long analysisId);
}
