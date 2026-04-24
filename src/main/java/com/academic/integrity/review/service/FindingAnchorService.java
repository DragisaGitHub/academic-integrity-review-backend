package com.academic.integrity.review.service;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.Finding;
import java.util.List;

public interface FindingAnchorService {

	void assignAnchors(Analysis analysis, List<Finding> findings);

	void backfillMissingAnchors(Analysis analysis);
}