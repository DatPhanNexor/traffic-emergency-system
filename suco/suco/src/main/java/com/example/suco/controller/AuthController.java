package com.example.suco.controller;

import com.example.suco.dto.AuthRequest;
import com.example.suco.model.User;
import com.example.suco.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.http.ResponseEntity;
import com.example.suco.service.UserService; // THÊM DÒNG NÀY ĐỂ HẾT LỖI
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {



    private final UserRepository userRepository;
    private final UserService userService; // 1. Thêm UserService vào đây

    // 2. Cập nhật Constructor để Inject UserService
    public AuthController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

@GetMapping("/all-users")
public ResponseEntity<?> getAllUsersForTest() {
    try {
        // Gọi service để lấy toàn bộ danh sách
        return ResponseEntity.ok(userService.getAllUsers());
    } catch (Exception e) {
        return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
    }
}
    @PostMapping("/sync")
public ResponseEntity<?> sync(@RequestBody AuthRequest request) {
    try {
        String uid;
        String email;
        String name;

        // --- CƠ CHẾ BYPASS ĐỂ TEST BẰNG POSTMAN ---
        if ("dev-token".equals(request.getToken())) {
            // fake token dùng cho Postman, sẽ tạo user tạm thời với thông tin cứng
    uid = "test-user";
    email = "test@gmail.com";
    name = "Test User";
} else {
            // Luồng thật dùng cho App Android
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(request.getToken());
            uid = decodedToken.getUid();
            email = decodedToken.getEmail();
            name = (String) decodedToken.getClaims().get("name");
        }
        // ------------------------------------------

        User user = userRepository.findById(uid).orElse(new User());
        
        user.setUid(uid);
        user.setEmail(email);
        user.setName(name);
        user.setProvider("google");

        userRepository.save(user);

        // Trả về thông tin đầy đủ (kèm gói cứu trợ nếu có)
        User userWithPackage = userService.getUserInfo(uid);
        return ResponseEntity.ok(userWithPackage);

    } catch (Exception e) {
        return ResponseEntity.status(401).body("Invalid Token: " + e.getMessage());
    }
}

        
@GetMapping("/me")
public ResponseEntity<?> getMyInfo(@RequestHeader("Authorization") String authHeader) {
    try {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(
                Map.of("error", "MISSING_TOKEN", "message", "Thiếu token")
            );
        }

        String token = authHeader.substring(7);

        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
        String uid = decodedToken.getUid();

        User user = userService.getUserInfo(uid);

        if (user == null) {
            return ResponseEntity.status(404).body(
                Map.of("error", "USER_NOT_FOUND", "message", "Không tìm thấy user")
            );
        }

        return ResponseEntity.ok(user);

    } catch (Exception e) {
        return ResponseEntity.status(401).body(
            Map.of("error", "INVALID_TOKEN", "message", e.getMessage())
        );
    }
}

     private boolean isValidUid(String uid) {
        return uid != null && uid.length() <= 256 && uid.matches("^[A-Za-z0-9_-]+$");
    }
    
}

// 4. SỬA TẠI ĐÂY: Gọi userService.getUserInfo thay vì userRepository
        // User user = userService.getUserInfo(uid);
        // if (user != null) {
        //     return ResponseEntity.ok(user);
        // }
        // return ResponseEntity.notFound().build();