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

    private static final String ADMIN_LOGIN_URL = "/admin/login";
    private static final String ADMIN_URL_PATTERN = "/admin/**";
    private static final String API_GOI_URL_PATTERN = "/api/goi/**";
    private static final String API_ADMIN_USER_URL_PATTERN = "/api/admin/quan-ly-user/**";
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
            // Sessio
            .securityContext(context -> context
        .requireExplicitSave(false)
    )
            .exceptionHandling(ex -> ex
            .authenticationEntryPoint(customAuthEntryPoint)
        )
            .authorizeHttpRequests(auth -> auth

                // PUBLIC
                .requestMatchers("/admin/login").permitAll()

                // ADMIN ONLY
                .requestMatchers("/admin/**").hasRole("ADMIN")// Chỉ Admin mới được vào
                .requestMatchers("/api/goi/**").hasRole("ADMIN")

                // USER + ADMIN đều vào được (có auth Firebase)

                // AUTH SYNC USER
                .requestMatchers("/api/auth/**").permitAll()
                //.requestMatchers("/api/map/**").authenticated()
                .requestMatchers("/api/goi/**").hasAuthority("ADMIN")

                        .requestMatchers(API_GOI_URL_PATTERN).hasRole(ADMIN_ROLE)
                        .requestMatchers(API_ADMIN_USER_URL_PATTERN).hasRole(ADMIN_ROLE)
                        .requestMatchers(API_AUTH_ALL_USERS_URL).hasRole(ADMIN_ROLE)
                        .requestMatchers(ADMIN_URL_PATTERN).hasRole(ADMIN_ROLE)

                        .requestMatchers(API_AUTH_URL_PATTERN).permitAll()
                        .requestMatchers(API_MAP_URL_PATTERN).authenticated()

                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(firebaseFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
// Đã hoàn thành kiểm thử Sprint 1 cho SVP-6
// Jira Link Test