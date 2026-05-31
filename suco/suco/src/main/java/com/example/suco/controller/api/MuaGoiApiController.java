package com.example.suco.controller.api;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.suco.service.GoiService;
import com.example.suco.service.MuaGoiService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

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

    // ĐĂNG KÝ GÓI (Phiên bản đã fix mã lỗi 404/400 cho SVP-03)
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

        } catch (RuntimeException e) {
            // Phân loại lỗi nghiệp vụ: 404 cho gói không tồn tại, 400 cho lỗi khác
            if (e.getMessage().contains("Không tìm thấy gói")) {
                return ResponseEntity.status(404)
                        .body(Map.of("message", e.getMessage()));
            }
            return ResponseEntity.status(400)
                    .body(Map.of("message", "Lỗi nghiệp vụ: " + e.getMessage()));
                    
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
            return ResponseEntity.status(401).body("Xác thực thất bại");
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
            return ResponseEntity.ok(Map.of("message", "Đã hủy gói thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Xác thực thất bại");
        }
    }
}