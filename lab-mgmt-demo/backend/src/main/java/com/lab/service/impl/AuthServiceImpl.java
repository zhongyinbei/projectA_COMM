package com.lab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.common.BizException;
import com.lab.dto.LoginRequest;
import com.lab.dto.LoginResponse;
import com.lab.dto.UserInfo;
import com.lab.entity.SysUser;
import com.lab.mapper.SysUserMapper;
import com.lab.service.AuthService;
import com.lab.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new BizException(400, "用户名和密码不能为空");
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername().trim()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        String token = jwtUtil.generate(user.getId(), user.getRole());
        return new LoginResponse(token, toUserInfo(user));
    }

    @Override
    public UserInfo me(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(401, "用户不存在或已被禁用");
        }
        return toUserInfo(user);
    }

    private UserInfo toUserInfo(SysUser user) {
        return new UserInfo(user.getId(), user.getUsername(), user.getRealName(), user.getRole());
    }
}
