package com.demo.app.api.mapper;

import com.demo.app.api.dto.UserDto;
import com.demo.app.domain.enums.Role;
import com.demo.app.domain.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole() != null ? user.getRole().name() : null,
                user.isEnabled()
        );
    }

    public User toModel(UserDto dto) {
        return User.builder()
                .id(dto.id())
                .username(dto.username())
                .email(dto.email())
                .displayName(dto.displayName())
                .role(dto.role() != null ? Role.valueOf(dto.role()) : null)
                .enabled(dto.enabled())
                .build();
    }
}
