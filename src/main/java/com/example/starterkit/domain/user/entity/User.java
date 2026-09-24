package com.example.starterkit.domain.user.entity;

import com.example.starterkit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 엔티티.
 * 상태 변경은 반드시 아래 도메인 메서드를 통하도록 하고, setter는 두지 않는다.
 */
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    private User(String email, String name) {
        this.email = email;
        this.name = name;
        this.status = UserStatus.ACTIVE;
    }

    /** 신규 사용자를 생성한다. 상태는 항상 ACTIVE로 시작한다. */
    public static User create(String email, String name) {
        return new User(email, name);
    }

    /** 이름을 변경한다. */
    public void changeName(String name) {
        this.name = name;
    }

    /** 사용자를 비활성 상태로 전환한다. */
    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }
}
