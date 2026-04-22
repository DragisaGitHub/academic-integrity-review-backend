package com.academic.integrity.review.mapper;

import com.academic.integrity.review.domain.Notification;
import com.academic.integrity.review.dto.NotificationResponseDTO;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

	NotificationResponseDTO toDto(Notification notification);

	List<NotificationResponseDTO> toDtoList(List<Notification> notifications);
}