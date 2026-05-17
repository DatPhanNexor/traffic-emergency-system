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
        String token = authHeader.replace("Bearer ", "");

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
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request
    ) {
        try {
            String uid = getUidFromHeader(authHeader);

            Long goiId = Long.valueOf(request.get("goiId").toString());
            muaGoiService.dangKyGoi(uid, goiId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Đăng ký gói thành công"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Xác thực thất bại: " + e.getMessage()));
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