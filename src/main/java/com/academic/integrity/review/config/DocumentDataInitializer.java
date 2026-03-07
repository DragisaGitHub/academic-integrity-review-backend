package com.academic.integrity.review.config;

import com.academic.integrity.review.domain.Document;
import com.academic.integrity.review.domain.ReviewPriority;
import com.academic.integrity.review.domain.ReviewStatus;
import com.academic.integrity.review.repository.DocumentRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(1)
public class DocumentDataInitializer implements CommandLineRunner {

	private final DocumentRepository documentRepository;

	@Override
	public void run(String... args) {
		if (documentRepository.count() > 0) {
			return;
		}

		Document doc1 = new Document();
		doc1.setTitle("Essay on Academic Integrity");
		doc1.setStudentName("Alex Student");
		doc1.setCourse("ENG-101");
		doc1.setSubmissionDate(LocalDate.now().minusDays(3));
		doc1.setReviewPriority(ReviewPriority.HIGH);
		doc1.setReviewStatus(ReviewStatus.PENDING);

		Document doc2 = new Document();
		doc2.setTitle("Lab Report: Data Analysis");
		doc2.setStudentName("Jamie Learner");
		doc2.setCourse("STAT-201");
		doc2.setSubmissionDate(LocalDate.now().minusDays(7));
		doc2.setReviewPriority(ReviewPriority.MEDIUM);
		doc2.setReviewStatus(ReviewStatus.IN_REVIEW);

		Document doc3 = new Document();
		doc3.setTitle("Research Summary: Ethics");
		doc3.setStudentName("Taylor Scholar");
		doc3.setCourse("PHIL-220");
		doc3.setSubmissionDate(LocalDate.now().minusDays(1));
		doc3.setReviewPriority(ReviewPriority.LOW);
		doc3.setReviewStatus(ReviewStatus.COMPLETED);

		documentRepository.saveAll(List.of(doc1, doc2, doc3));
	}
}
