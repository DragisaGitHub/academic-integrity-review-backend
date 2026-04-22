package com.academic.integrity.review.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindingUpdateRequestDTO {
	private String professorNotes;
	private Boolean reviewed;
	private Boolean flaggedForFollowUp;
}