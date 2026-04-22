package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.domain.Analysis;
import com.academic.integrity.review.domain.Document;
import com.academic.integrity.review.domain.Notification;
import com.academic.integrity.review.dto.NotificationListResponseDTO;
import com.academic.integrity.review.dto.NotificationResponseDTO;
import com.academic.integrity.review.exception.ResourceNotFoundException;
import com.academic.integrity.review.mapper.NotificationMapper;
import com.academic.integrity.review.repository.NotificationRepository;
import com.academic.integrity.review.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

	private static final int DEFAULT_LIMIT = 10;

	private final NotificationRepository notificationRepository;
	private final NotificationMapper notificationMapper;

	@Override
	@Transactional(readOnly = true)
	public NotificationListResponseDTO getNotifications(int limit) {
		int resolvedLimit = limit > 0 ? limit : DEFAULT_LIMIT;
		List<NotificationResponseDTO> notifications = notificationMapper.toDtoList(
				notificationRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, resolvedLimit)));
		long unreadCount = notificationRepository.countByReadFalse();
		return new NotificationListResponseDTO(notifications, unreadCount);
	}

	@Override
	@Transactional
	public void markAsRead(String notificationId) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new ResourceNotFoundException("Notification not found: id=" + notificationId));
		notification.setRead(true);
		notificationRepository.save(notification);
	}

	@Override
	@Transactional
	public void markAllAsRead() {
		notificationRepository.markAllUnreadAsRead();
	}

	@Override
	@Transactional
	public void createAnalysisCompletedNotification(Analysis analysis) {
		Document document = analysis.getDocument();
		Notification notification = buildBaseNotification(analysis, document);
		notification.setType("analysis-completed");
		notification.setTitle("Analysis complete");
		notification.setMessage(documentTitle(document) + " — analysis finished successfully");
		notification.setSeverity("success");
		notificationRepository.save(notification);
	}

	@Override
	@Transactional
	public void createAnalysisFailedNotification(Analysis analysis, String errorMessage) {
		Document document = analysis.getDocument();
		Notification notification = buildBaseNotification(analysis, document);
		notification.setType("analysis-failed");
		notification.setTitle("Analysis failed");
		notification.setMessage(documentTitle(document) + " — " + failureText(errorMessage));
		notification.setSeverity("error");
		notificationRepository.save(notification);
	}

	private static Notification buildBaseNotification(Analysis analysis, Document document) {
		Notification notification = new Notification();
		notification.setRead(false);
		notification.setDocumentId(document != null ? document.getId() : null);
		notification.setAnalysisId(analysis.getId());
		notification.setRoute("/analysis/" + (document != null ? document.getId() : ""));
		return notification;
	}

	private static String documentTitle(Document document) {
		if (document == null || document.getTitle() == null || document.getTitle().isBlank()) {
			return "Document";
		}
		return document.getTitle();
	}

	private static String failureText(String errorMessage) {
		return (errorMessage == null || errorMessage.isBlank()) ? "analysis failed" : errorMessage;
	}
}