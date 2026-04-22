package com.academic.integrity.review.controller;

import com.academic.integrity.review.dto.NotificationListResponseDTO;
import com.academic.integrity.review.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	@GetMapping
	public ResponseEntity<NotificationListResponseDTO> getNotifications(
			@RequestParam(defaultValue = "10") int limit) {
		return ResponseEntity.ok(notificationService.getNotifications(limit));
	}

	@PatchMapping("/{id}/read")
	public ResponseEntity<Void> markAsRead(@PathVariable String id) {
		notificationService.markAsRead(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/read-all")
	public ResponseEntity<Void> markAllAsRead() {
		notificationService.markAllAsRead();
		return ResponseEntity.noContent().build();
	}
}