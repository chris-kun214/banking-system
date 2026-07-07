package com.banking.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 只保护 /api/internal/** 路径。这些端点供 EventBridge 触发的 Lambda 调用（每日对账、月度报告批量生成），
 * 不走面向用户的 JWT 认证，改用一个共享密钥请求头。api-key 未配置时一律拒绝（fail closed）。
 */
@Component
@Order(0)
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Internal-Api-Key";
    private static final String INTERNAL_PATH_PREFIX = "/api/internal/";

    @Value("${internal.api-key:}")
    private String configuredApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(HEADER_NAME);
        if (configuredApiKey == null || configuredApiKey.isBlank()
                || providedKey == null || !providedKey.equals(configuredApiKey)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"invalid or missing internal api key\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
