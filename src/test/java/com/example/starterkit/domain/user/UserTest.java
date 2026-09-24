package com.example.starterkit.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.starterkit.domain.user.entity.User;
import com.example.starterkit.domain.user.entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** User 도메인 규칙 단위 테스트. 스프링 컨텍스트나 Docker 없이 동작한다. */
class UserTest {

    @Test
    @DisplayName("생성된 사용자는 ACTIVE 상태로 시작한다")
    void createStartsActive() {
        User user = User.create("test@example.com", "홍길동");

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("이름을 변경하면 반영된다")
    void changeName() {
        User user = User.create("test@example.com", "홍길동");

        user.changeName("김철수");

        assertThat(user.getName()).isEqualTo("김철수");
    }

    @Test
    @DisplayName("비활성화하면 상태가 INACTIVE로 바뀐다")
    void deactivate() {
        User user = User.create("test@example.com", "홍길동");

        user.deactivate();

        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
    }
}
