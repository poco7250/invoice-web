package com.example.starterkit.domain.user.mapper;

import com.example.starterkit.domain.user.dto.UserResponse;
import com.example.starterkit.domain.user.entity.User;
import org.mapstruct.Mapper;

/**
 * User 엔티티 -> 응답 DTO 매핑.
 * 엔티티 생성은 User.create() 도메인 팩토리를 쓰므로 역방향 매핑은 두지 않는다.
 * componentModel은 build.gradle.kts의 컴파일 옵션(-Amapstruct.defaultComponentModel=spring)으로 지정된다.
 */
@Mapper
public interface UserMapper {

    UserResponse toResponse(User user);
}
