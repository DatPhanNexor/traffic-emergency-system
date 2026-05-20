package com.example.suco.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.suco.security.CustomAuthEntryPoint;
import com.example.suco.security.FirebaseFilter;
import com.example.suco.security.JwtFilter;

@Configuration
public class SecurityConfig {

    private final FirebaseFilter firebaseFilter;
    private final JwtFilter jwtFilter;
    private final CustomAuthEntryPoint customAuthEntryPoint;

    public SecurityConfig(FirebaseFilter firebaseFilter,
                      JwtFilter jwtFilter,
                      CustomAuthEntryPoint customAuthEntryPoint) {
    this.firebaseFilter = firebaseFilter;
    this.jwtFilter = jwtFilter;
    this.customAuthEntryPoint = customAuthEntryPoint;
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
                .requestMatchers("/api/map/**").authenticated()

                .anyRequest().permitAll()
            )

            // JWT ADMIN chạy trước
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

            // Firebase USER chạy sau
            .addFilterBefore(firebaseFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
// Đã hoàn thành kiểm thử Sprint 1 cho SVP-6
// Jira Link Test