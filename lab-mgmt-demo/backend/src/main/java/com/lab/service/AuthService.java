package com.lab.service;

import com.lab.dto.LoginRequest;
import com.lab.dto.LoginResponse;
import com.lab.dto.UserInfo;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    UserInfo me(Long userId);
}
