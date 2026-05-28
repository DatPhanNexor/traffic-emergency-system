package com.example.suco.controller.admin;

import com.example.suco.model.User;
import com.example.suco.repository.UserRepository;
import com.example.suco.security.JwtService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminAuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<?> login(HttpServletRequest request) throws IOException {
        Map<String, String> req = readLoginRequest(request);

        String email = normalize(req.get("email"));
        String password = req.get("password");

        if (isBlank(email) || isBlank(password)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Email và mật khẩu không được để trống"
            ));
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

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

        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(403).body(Map.of(
                    "message", "Tài khoản không có quyền admin"
            ));
        }

        String token = jwtService.generateToken(user.getUid(), "ADMIN");

        ResponseCookie cookie = ResponseCookie.from("ADMIN_JWT", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of(
                        "message", "Đăng nhập admin thành công",
                        "token", token,
                        "uid", user.getUid(),
                        "role", "ADMIN"
                ));
    }

    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }

    private Map<String, String> readLoginRequest(HttpServletRequest request) throws IOException {
        String contentType = request.getContentType();

        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            try {
                Map<String, String> body = objectMapper.readValue(
                        request.getInputStream(),
                        new TypeReference<Map<String, String>>() {}
                );
                return body != null ? body : new HashMap<>();
            } catch (Exception ex) {
                return new HashMap<>();
            }
        }

        Map<String, String> form = new HashMap<>();
        form.put("email", request.getParameter("email"));
        form.put("password", request.getParameter("password"));
        return form;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(401).body(Map.of(
                "message", "Sai tài khoản hoặc mật khẩu"
        ));
    }
}