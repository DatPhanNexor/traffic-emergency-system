package com.example.suco.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class FirebaseFilter extends OncePerRequestFilter {

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
    
        String path = request.getRequestURI();
        String auth = request.getHeader("Authorization");


        // CHỈ API USER
        if (!path.startsWith("/api") || path.startsWith("/api/goi")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (auth == null || !auth.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
    filterChain.doFilter(request, response);
    return;
}


        String token = auth.substring(7);

        // JWT thì bỏ qua
        if (token.split("\\.").length == 3) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);

            String uid = decoded.getUid();

            var authentication = new UsernamePasswordAuthenticationToken(
                    uid,
                    null,
                    List.of()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            SecurityContextHolder.clearContext();
filterChain.doFilter(request, response);
return; 
        }

        filterChain.doFilter(request, response);
    }
}