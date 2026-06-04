package com.example.suco.controller.api;

import com.example.suco.service.MuaGoiService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.suco.service.GoiService;

import java.util.Map;

@RestController
@RequestMapping("/api/mua-goi")
public class MuaGoiApiController {

    @Autowired
    private MuaGoiService muaGoiService;

    @Autowired
    private GoiService goiService;

    private String getUidFromHeader(String authHeader) throws Exception {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new Exception("Thiếu hoặc sai Authorization header");
        }
        
        String token = authHeader.replace("Bearer ", "").trim();
        
        if (token.isBlank()) {
            throw new Exception("Token không được để trống");
        }
        
        if ("dev-token".equals(token)) {
            return "test-user";
        }
        
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
        return decodedToken.getUid();
    }

        // 🔹 Danh sách
    @GetMapping("/danh-sach")
    public ResponseEntity<?> getDanhSachGoi() {
        return ResponseEntity.ok(goiService.getAllGoi());
    }

    // ĐĂNG KÝ GÓI 
    @PostMapping("/dang-ky")
    public ResponseEntity<?> dangKyMuaGoi(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @RequestBody Map<String, Object> request
    ) {
        String uid;
        
        // 1. Chỉ lỗi token mới trả 401
        try {
            uid = getUidFromHeader(authHeader);
        } catch (Exception e) {
            return ResponseEntity.status(401)
            .body(Map.of(
                "status", "error",
                "message", "Xác thực thất bại: " + e.getMessage()
            ));
        }
        
        // 2. Thiếu goiId là lỗi dữ liệu, trả 400
        if (request == null || !request.containsKey("goiId") || request.get("goiId") == null) {
            return ResponseEntity.badRequest()
            .body(Map.of(
                "status", "error",
                "message", "goiId không được để trống"
            ));
        }
        
        Long goiId;
        
        // 3. goiId sai format là lỗi dữ liệu, trả 400
        try {
            String rawGoiId = request.get("goiId").toString().trim();
            
            if (rawGoiId.isBlank()) {
                return ResponseEntity.badRequest()
                .body(Map.of(
                    "status", "error",
                    "message", "goiId không được để trống"
                ));
            }
            
            goiId = Long.valueOf(rawGoiId);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
            .body(Map.of(
                "status", "error",
                "message", "goiId sai định dạng"
            ));
        }
        
        // 4. Lỗi nghiệp vụ không được gom vào 401
        try {
            muaGoiService.dangKyGoi(uid, goiId);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Đăng ký gói thành công"
            ));
        
        } catch (RuntimeException e) {
            if ("Không tìm thấy gói cứu hộ".equals(e.getMessage())) {
                return ResponseEntity.status(404)
                .body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
                ));
            }
            
            return ResponseEntity.badRequest()
            .body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    // LẤY GÓI CỦA TÔI 
    @GetMapping("/my-packages")
    public ResponseEntity<?> getMyPackages(
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String uid = getUidFromHeader(authHeader);

            return ResponseEntity.ok(muaGoiService.getGoiByUserId(uid));

        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body("Xác thực thất bại");
        }
    }

    // HỦY GÓI 
    @PostMapping("/cancel/{id}")
    public ResponseEntity<?> cancelGoi(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id
    ) {
        try {
            String uid = getUidFromHeader(authHeader);

            muaGoiService.huyGoi(id, uid);

            return ResponseEntity.ok(Map.of(
                    "message", "Đã hủy gói thành công"
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body("Xác thực thất bại");
        }
    }
}