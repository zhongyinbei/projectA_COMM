package com.lab.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.common.BizException;
import com.lab.common.Result;
import com.lab.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeError(response, 401, "未登录或登录已过期");
            return false;
        }

        try {
            Claims claims = jwtUtil.parse(authorization.substring(7));
            Long userId = Long.valueOf(claims.getSubject());
            String role = claims.get("role", String.class);
            if (role == null) {
                writeError(response, 401, "登录凭证无效");
                return false;
            }
            CURRENT_USER.set(new CurrentUser(userId, role));
        } catch (Exception e) {
            writeError(response, 401, "未登录或登录已过期");
            return false;
        }

        if (handler instanceof HandlerMethod handlerMethod) {
            RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
            if (requireRole == null) {
                requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
            }
            if (requireRole != null && Arrays.stream(requireRole.value()).noneMatch(role -> role.equals(currentRole()))) {
                writeError(response, 403, "无权限访问");
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CURRENT_USER.remove();
    }

    private void writeError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(status, message)));
    }

    public static CurrentUser currentUser() {
        CurrentUser user = CURRENT_USER.get();
        if (user == null) {
            throw new BizException(401, "未登录或登录已过期");
        }
        return user;
    }

    public static Long currentUserId() {
        return currentUser().id();
    }

    public static String currentRole() {
        return currentUser().role();
    }

    public record CurrentUser(Long id, String role) {
    }
}
