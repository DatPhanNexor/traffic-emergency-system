package com.example.suco.controller.api;

import com.example.suco.dto.DoiQuaDto;
import com.example.suco.repository.DoiQuaRepository;
import com.example.suco.service.DoiQuaService;
import com.example.suco.repository.QuaRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/qua")
public class DoiQuaApiController {

    @Autowired
    private DoiQuaService doiQuaService;

    @Autowired
    private DoiQuaRepository doiQuaRepository;

    @Autowired
    private QuaRepository quaRepository;

    // ================== HÀM LẤY UID ==================
    private String getUidFromHeader(String authHeader) throws Exception {
        String token = authHeader.replace("Bearer ", "");

        if ("dev-token".equals(token)) {
            return "test-user"; // nhớ tạo user này trong DB
        }

        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
        return decodedToken.getUid();
    }

    // ================== 1. ĐỔI QUÀ ==================
    @PostMapping("/exchange")
public ResponseEntity<?> exchange(
        @RequestHeader("Authorization") String authHeader,
        @RequestBody DoiQuaDto dto
) {
    try {
        String uid = getUidFromHeader(authHeader);

        doiQuaService.thucHienDoiQua(uid, dto);

        return ResponseEntity.ok(Map.of(
                "message", "Đổi quà thành công!"
        ));

    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "message", e.getMessage()
        ));

    } catch (Exception e) {
        return ResponseEntity.status(401).body(Map.of(
                "message", "Xác thực thất bại"
        ));
    }
}

    // ================== 2. TÚI QUÀ CỦA TÔI ==================
 @GetMapping("/my-gifts")
public ResponseEntity<?> getMyGifts(@RequestHeader("Authorization") String authHeader) {
    try {
        // 1. Trích xuất UID từ header (giữ nguyên hàm helper của bạn)
        String uid = getUidFromHeader(authHeader);

        // 2. Gọi Service để xử lý tác vụ
        List<DoiQuaDto> result = doiQuaService.getMyGifts(uid);

        // 3. Trả về kết quả
        return ResponseEntity.ok(result);

    } catch (Exception e) {
        return ResponseEntity.status(401).body("Xác thực thất bại");
    }
}
}