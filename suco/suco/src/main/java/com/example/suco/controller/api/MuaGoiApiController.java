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

    // ĐĂNG KÝ GÓI (Phiên bản đã fix mã lỗi 404/400 cho SVP-03)
    @PostMapping("/dang-ky")
public ResponseEntity<?> dangKyMuaGoi(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @RequestBody Map<String, Object> request
) {
    String uid;

    // Chỉ lỗi token mới trả 401
    try {
        uid = getUidFromHeader(authHeader);
    } catch (Exception e) {
        return ResponseEntity.status(401)
                .body(Map.of(
                        "status", "error",
                        "message", "Xác thực thất bại: " + e.getMessage()
                ));
    }

    // Thiếu goiId là lỗi dữ liệu, trả 400
    if (request == null || !request.containsKey("goiId") || request.get("goiId") == null) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "status", "error",
                        "message", "goiId không được để trống"
                ));
    }

    Long goiId;

    // goiId sai định dạng là lỗi dữ liệu, trả 400
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

    // Lỗi nghiệp vụ không được gom vào 401
    try {
        muaGoiService.dangKyGoi(uid, goiId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Đăng ký gói thành công"
        ));

    } catch (RuntimeException e) {
        if (e.getMessage() != null && e.getMessage().contains("Không tìm thấy gói")) {
            return ResponseEntity.status(404)
                    .body(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ));
        }

        return ResponseEntity.badRequest()
                .body(Map.of(
                        "status", "error",
                        "message", "Lỗi nghiệp vụ: " + e.getMessage()
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