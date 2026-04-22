package com.academic.integrity.review.dto;

import com.academic.integrity.review.domain.ReviewPriority;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadRequestDTO {
	private String title;
	private String studentName;
	private String course;
	private String academicYear;
	private ReviewPriority reviewPriority;
}
