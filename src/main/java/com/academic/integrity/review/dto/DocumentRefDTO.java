package com.academic.integrity.review.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRefDTO {
	private Long id;
	private String title;
	private String studentName;
	private String course;
}