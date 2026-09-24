package com.example.starterkit.domain.user.service;

import com.example.starterkit.domain.user.dto.UserCreateRequest;
import com.example.starterkit.domain.user.dto.UserResponse;
import com.example.starterkit.domain.user.dto.UserUpdateRequest;
import com.example.starterkit.domain.user.entity.User;
import com.example.starterkit.domain.user.mapper.UserMapper;
import com.example.starterkit.domain.user.repository.UserRepository;
import com.example.starterkit.global.exception.BusinessException;
import com.example.starterkit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 비즈니스 로직.
 * 트랜잭션 경계는 이 Service 레이어에만 둔다. (Controller / Repository에는 두지 않음)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /** 사용자를 생성한다. 이메일이 이미 있으면 DUPLICATE_EMAIL 예외를 던진다. */
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User saved = userRepository.save(User.create(request.email(), request.name()));
        log.info("사용자 생성 완료: id={}", saved.getId());

        return userMapper.toResponse(saved);
    }

    /** 단건 조회. */
    public UserResponse findById(Long id) {
        return userMapper.toResponse(getUserOrThrow(id));
    }

    /** 페이지 단위 목록 조회. */
    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    /** 이름을 수정한다. 변경 감지(dirty checking)로 반영되므로 save 호출이 없다. */
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = getUserOrThrow(id);
        user.changeName(request.name());
        log.info("사용자 수정 완료: id={}", id);

        return userMapper.toResponse(user);
    }

    /** 사용자를 비활성 처리한다. (물리 삭제 대신 소프트 삭제) */
    @Transactional
    public void deactivate(Long id) {
        getUserOrThrow(id).deactivate();
        log.info("사용자 비활성 처리 완료: id={}", id);
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
