package com.example.starterkit.domain.user.controller;

import com.example.starterkit.domain.user.dto.UserCreateRequest;
import com.example.starterkit.domain.user.dto.UserResponse;
import com.example.starterkit.domain.user.dto.UserUpdateRequest;
import com.example.starterkit.domain.user.service.UserService;
import com.example.starterkit.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 API.
 * 컨트롤러는 요청/응답 변환만 담당하고 비즈니스 로직은 Service에 위임한다.
 */
@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "사용자 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok(userService.create(request));
    }

    @Operation(summary = "사용자 단건 조회")
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(userService.findById(id));
    }

    @Operation(summary = "사용자 목록 조회")
    @GetMapping
    public ApiResponse<Page<UserResponse>> findAll(Pageable pageable) {
        return ApiResponse.ok(userService.findAll(pageable));
    }

    @Operation(summary = "사용자 이름 수정")
    @PatchMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable Long id,
                                            @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.ok(userService.update(id, request));
    }

    @Operation(summary = "사용자 비활성화")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        userService.deactivate(id);
        return ApiResponse.ok();
    }
}
