-- 사용자 테이블
CREATE TABLE users
(
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    name       VARCHAR(100) NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX idx_users_status ON users (status);

COMMENT ON TABLE users IS '사용자';
COMMENT ON COLUMN users.email IS '이메일 (로그인 식별자)';
COMMENT ON COLUMN users.status IS '상태: ACTIVE, INACTIVE';
