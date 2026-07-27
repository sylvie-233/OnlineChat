package com.sylvie233.server.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器（基于 Sa-Token）
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        // 白名单路径放行
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/") || path.startsWith("/doc.html")
                || path.startsWith("/v3/api-docs") || path.startsWith("/webjars")) {
            return true;
        }

        // 检查登录
        try {
            StpUtil.checkLogin();
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":401,\"message\":\"未登录\"}");
            } catch (Exception ignored) {}
            return false;
        }
    }
}
