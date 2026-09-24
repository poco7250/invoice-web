package com.example.starterkit.domain.user.dto;

import com.example.starterkit.domain.user.entity.UserStatus;
import java.time.LocalDateTime;

/**
 * 사용자 응답.
 * 엔티티를 직접 노출하지 않기 위해 별도 record로 분리한다.
 */
public record UserResponse(
        Long id,
        String email,
        String name,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
