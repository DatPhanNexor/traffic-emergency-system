package com.example.suco.controller.admin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.suco.model.User;
import com.example.suco.repository.UserRepository;
import com.example.suco.security.JwtService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin")
public class AdminAuthController {

    private static final String EMAIL_KEY = "email";
    private static final String PASSWORD_KEY = "password";
    private static final String MESSAGE_KEY = "message";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ADMIN_JWT_COOKIE = "ADMIN_JWT";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String TOKEN_KEY = "token";
    private static final String UID_KEY = "uid";
    private static final String ROLE_KEY = "role";

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public AdminAuthController(
            UserRepository userRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = new ObjectMapper();
    }

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> login(HttpServletRequest request) throws IOException {
        Map<String, String> req = readLoginRequest(request);

        String email = normalize(req.get(EMAIL_KEY));
        String password = req.get(PASSWORD_KEY);

        if (isBlank(email) || isBlank(password)) {
            return ResponseEntity.badRequest().body(Map.of(
                    MESSAGE_KEY, "Email và mật khẩu không được để trống"
            ));
        }

        User user = userRepository.findAll()
            .stream()
            .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email))
            .findFirst()
            .orElse(null);

        if (user == null) {
            return unauthorized();
        }

        String hashedPassword = user.getPassword();

        if (isBlank(hashedPassword)) {
            return unauthorized();
        }

        boolean passwordMatched;
        try {
            passwordMatched = passwordEncoder.matches(password, hashedPassword.trim());
        } catch (IllegalArgumentException ex) {
            passwordMatched = false;
        }

        if (!passwordMatched) {
            return unauthorized();
        }

        String role = normalize(user.getRole()).toUpperCase();

        if (!ADMIN_ROLE.equals(role)) {
            return ResponseEntity.status(403).body(Map.of(
                    MESSAGE_KEY, "Tài khoản không có quyền admin"
            ));
        }

        String token = jwtService.generateToken(user.getUid(), ADMIN_ROLE);

        ResponseCookie cookie = ResponseCookie.from(ADMIN_JWT_COOKIE, token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of(
                    MESSAGE_KEY, "Đăng nhập admin thành công",
                    TOKEN_KEY, token,
                    UID_KEY, user.getUid(),
                    ROLE_KEY, ADMIN_ROLE
                ));
    }

    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }

    private Map<String, String> readLoginRequest(HttpServletRequest request) throws IOException {
        String contentType = request.getContentType();

        if (contentType != null && contentType.toLowerCase().contains(JSON_CONTENT_TYPE)) {
            try {
                Map<String, String> body = objectMapper.readValue(
                        request.getInputStream(),
                        new TypeReference<>() {}
                );
                return body != null ? body : new HashMap<>();
            } catch (Exception ex) {
                return new HashMap<>();
            }
        }

        Map<String, String> form = new HashMap<>();
        form.put(EMAIL_KEY, request.getParameter(EMAIL_KEY));
        form.put(PASSWORD_KEY, request.getParameter(PASSWORD_KEY));
        return form;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private ResponseEntity<Map<String, Object>> unauthorized() {
        return ResponseEntity.status(401).body(Map.of(
                MESSAGE_KEY, "Sai tài khoản hoặc mật khẩu"
        ));
    }
}