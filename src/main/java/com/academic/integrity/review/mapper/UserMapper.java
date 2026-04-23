package com.academic.integrity.review.mapper;

import com.academic.integrity.review.domain.User;
import com.academic.integrity.review.dto.UserResponseDTO;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

	UserResponseDTO toDto(User user);

	List<UserResponseDTO> toDtoList(List<User> users);
}