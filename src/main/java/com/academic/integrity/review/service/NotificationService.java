package com.academic.integrity.review.service;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.dto.NotificationListResponseDTO;

public interface NotificationService {

	NotificationListResponseDTO getNotifications(int limit);

	void markAsRead(String notificationId);

	void markAllAsRead();

	void createAnalysisCompletedNotification(Analysis analysis);

	void createAnalysisFailedNotification(Analysis analysis, String errorMessage);
}