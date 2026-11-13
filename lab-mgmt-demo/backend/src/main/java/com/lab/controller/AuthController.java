package com.lab.controller;

import com.lab.common.Result;
import com.lab.dto.LoginRequest;
import com.lab.dto.LoginResponse;
import com.lab.dto.UserInfo;
import com.lab.interceptor.JwtInterceptor;
import com.lab.interceptor.RequireRole;
import com.lab.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @GetMapping("/me")
    @RequireRole({"ADMIN", "REPAIRER", "REPORTER"})
    public Result<UserInfo> me() {
        return Result.ok(authService.me(JwtInterceptor.currentUserId()));
    }
}
