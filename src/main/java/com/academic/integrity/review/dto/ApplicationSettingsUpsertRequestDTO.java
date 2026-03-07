package com.academic.integrity.review.dto;

import com.academic.integrity.review.domain.ReadingLayout;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSettingsUpsertRequestDTO {
	private String professorName;
	private String department;
	private String university;
	private boolean localAiEnabled;
	private int documentRetentionDays;
	private String storageLocation;
	private boolean lightThemeEnabled;
	private ReadingLayout readingLayout;
}
