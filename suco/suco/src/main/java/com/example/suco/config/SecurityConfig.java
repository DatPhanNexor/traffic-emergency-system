package com.example.suco.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.suco.security.CustomAuthEntryPoint;
import com.example.suco.security.FirebaseFilter;
import com.example.suco.security.JwtFilter;

@Configuration
public class SecurityConfig {

    private static final String ADMIN_URL_PATTERN = "/admin/**";
    private static final String ADMIN_LOGIN_URL = "/admin/login";
    private static final String API_GOI_URL_PATTERN = "/api/goi/**";
    private static final String API_ADMIN_USER_URL_PATTERN = "/api/admin/users/**";
    private static final String API_AUTH_ALL_USERS_URL = "/api/auth/all-users";
    private static final String API_AUTH_URL_PATTERN = "/api/auth/**";
    private static final String API_MAP_URL_PATTERN = "/api/map/**";
    private static final String ADMIN_ROLE = "ADMIN";

    private final FirebaseFilter firebaseFilter;
    private final JwtFilter jwtFilter;
    private final CustomAuthEntryPoint customAuthEntryPoint;

    public SecurityConfig(
            FirebaseFilter firebaseFilter,
            JwtFilter jwtFilter,
            CustomAuthEntryPoint customAuthEntryPoint
    ) {
        this.firebaseFilter = firebaseFilter;
        this.jwtFilter = jwtFilter;
        this.customAuthEntryPoint = customAuthEntryPoint;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .securityContext(context -> context
                .requireExplicitSave(false)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(customAuthEntryPoint)
            )
            .authorizeHttpRequests(auth -> auth
                // 1. PUBLIC: Các đường dẫn ai cũng vào được
                .requestMatchers(ADMIN_LOGIN_URL).permitAll() 
                .requestMatchers(API_AUTH_URL_PATTERN).permitAll()

                // 2. ADMIN ONLY: Sử dụng Hằng số (Constants) để fix lỗi Sonar
                .requestMatchers(ADMIN_URL_PATTERN).hasRole(ADMIN_ROLE)
                .requestMatchers(API_GOI_URL_PATTERN).hasRole(ADMIN_ROLE)
                .requestMatchers(API_ADMIN_USER_URL_PATTERN).hasRole(ADMIN_ROLE)
                .requestMatchers(API_AUTH_ALL_USERS_URL).hasRole(ADMIN_ROLE)

                // 3. USER + ADMIN: Yêu cầu đăng nhập nói chung
                .requestMatchers(API_MAP_URL_PATTERN).authenticated()

                // 4. Các yêu cầu khác
                .anyRequest().permitAll()
            )
            // JWT ADMIN chạy trước để kiểm tra token trong Header/Cookie
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            // Firebase USER chạy sau
            .addFilterBefore(firebaseFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
// Đã hoàn thành kiểm thử Sprint 1 cho SVP-6
// Jira Link Test: SVP-6, SVP-55, SVP-56