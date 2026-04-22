package com.academic.integrity.review.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListResponseDTO {
	private List<NotificationResponseDTO> notifications;
	private long unreadCount;
}