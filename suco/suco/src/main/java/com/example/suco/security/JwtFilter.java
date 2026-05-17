package com.example.suco.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

   @Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain)
        throws ServletException, IOException {

                String contentType = request.getContentType();
    if (contentType != null && contentType.startsWith("multipart/")) {
        filterChain.doFilter(request, response);
        return;
    }
    String token = resolveToken(request);

    if (token == null || token.isBlank()) {
        filterChain.doFilter(request, response);
        return;
    }

    try {
        Claims claims = jwtService.extractAllClaims(token);

        String role = (String) claims.get("role");

        var authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role)
        );

        var authentication = new UsernamePasswordAuthenticationToken(
                claims.getSubject(),
                null,
                authorities
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

    } catch (Exception e) {
        SecurityContextHolder.clearContext();
        filterChain.doFilter(request, response);
        return;
    }

    filterChain.doFilter(request, response);
}

    private String resolveToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if ("ADMIN_JWT".equals(cookie.getName()) && cookie.getValue() != null) {
                return cookie.getValue();
            }
        }

        return null;
    }
}