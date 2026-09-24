package com.example.starterkit.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 사용자 수정 요청. 이메일은 식별자 성격이라 변경 대상에서 제외한다.
 *
 * @param name 변경할 이름
 */
public record UserUpdateRequest(

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 100, message = "이름은 100자를 넘을 수 없습니다.")
        String name
) {
}
