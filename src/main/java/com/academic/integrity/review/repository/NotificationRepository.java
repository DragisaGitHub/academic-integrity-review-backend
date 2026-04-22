package com.academic.integrity.review.repository;

import com.academic.integrity.review.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {
	List<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

	long countByReadFalse();

	@Modifying
	@Query("update Notification n set n.read = true where n.read = false")
	void markAllUnreadAsRead();
}