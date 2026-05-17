package com.example.suco.controller.admin;
import com.example.suco.model.User;
import com.example.suco.repository.UserRepository;
import com.example.suco.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminAuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/login")
    @ResponseBody
public ResponseEntity<?> login(@RequestBody Map<String, String> req) {

    String email = req.get("email");
    String password = req.get("password");

    User user = userRepository.findByEmail(email).orElse(null);

    if (user == null) {
        return ResponseEntity.status(401).body("Sai tài khoản hoặc mật khẩu");
    }

    if (!passwordEncoder.matches(password, user.getPassword())) {
        return ResponseEntity.status(401).body("Sai tài khoản hoặc mật khẩu");
    }

    if (!"ADMIN".equals(user.getRole())) {
        return ResponseEntity.status(401).body("Không phải admin");
    }

    String token = jwtService.generateToken(user.getUid(), user.getRole());
    ResponseCookie cookie = ResponseCookie.from("ADMIN_JWT", token)
            .httpOnly(true)
            .secure(false)
            .path("/")
            .sameSite("Lax")
            .build();

    return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(Map.of("token", token));
}
@GetMapping("/login")
public String loginPage() {
    return "admin/login";
}

}