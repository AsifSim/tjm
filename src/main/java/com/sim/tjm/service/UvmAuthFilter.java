package com.sim.tjm.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class UvmAuthFilter extends OncePerRequestFilter {

    @Autowired
    private UvmTokenService tokenService;

    protected void doFilterInternal_old(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. BYPASS LOGIC: Allow UI (static files) and /auth endpoints to pass through without a token
        if (!path.startsWith("/api/") || path.startsWith("/api/v1/uvm/auth")) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Extract Token from Header (UI) or Cookie (Console)
        String token = extractToken(request);

        // 3. Validate Token
        if (tokenService.isTokenValid(token)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized. Please connect and authenticate first.\"}");
        }
    }

    private String extractToken(HttpServletRequest request) {
        // Check UI Header: "Authorization: Bearer <token>"
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        // Check Console Cookie: "uvm_token=<token>"
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("uvm_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    // Inject the manager at the top of UvmAuthFilter
    @Autowired
    private DynamicApiManager dynamicApiManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // BYPASS LOGIC UPDATE
        boolean isDynamicApi = dynamicApiManager.isDynamicRoute(path);
        boolean isAuthEndpoint = path.startsWith("/api/v1/uvm/auth");
        boolean isStaticFile = !path.startsWith("/api/") && !isDynamicApi; // Static files aren't dynamic APIs

        if (isStaticFile || isAuthEndpoint) {
            chain.doFilter(request, response);
            return;
        }

        // Require token for standard /api/ endpoints AND any newly generated dynamic APIs
        String token = extractToken(request);

        if (tokenService.isTokenValid(token)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized. Please connect and authenticate first.\"}");
        }
    }
}