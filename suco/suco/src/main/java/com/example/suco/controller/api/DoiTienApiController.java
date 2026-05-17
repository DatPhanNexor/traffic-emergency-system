package com.example.suco.controller.api;

import com.example.suco.dto.DoiTienDto;
import com.example.suco.dto.ThongKeQuyDto;
import com.example.suco.model.DoiTien;
import com.example.suco.model.User;
import com.example.suco.repository.DoiTienRepository;
import com.example.suco.repository.UserRepository;
import com.example.suco.service.DoiTienService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doi-tien")
public class DoiTienApiController {

    @Autowired private DoiTienService doiTienService;
    @Autowired private DoiTienRepository doiTienRepository;
    @Autowired private UserRepository userRepository;

    private String getUidFromHeader(String authHeader) throws Exception {
    String token = authHeader.replace("Bearer ", "");

    if ("dev-token".equals(token)) {
        return "test-user";
    }

    FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
    return decodedToken.getUid();
}

   @PostMapping("/thuc-hien")
public ResponseEntity<?> thucHienDoi(
        @RequestHeader("Authorization") String authHeader,
        @RequestBody DoiTienDto dto
) {
    try {
        String uid = getUidFromHeader(authHeader);

        doiTienService.thucHienDoiTien(uid, dto);

        return ResponseEntity.ok(Map.of(
                "message", "Đổi tiền thành công!"
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

    @GetMapping("/thong-ke-quy")
public ResponseEntity<ThongKeQuyDto> getThongKeQuy() {
    Long tongGiaTri = doiTienRepository.sumAllDonationValues();
    if (tongGiaTri == null) tongGiaTri = 0L;

    // Sử dụng hàm helper đã tạo ở Bước 1
    List<Map<String, Object>> vinhDanh = doiTienService.getFormattedVinhDanh();

    return ResponseEntity.ok(new ThongKeQuyDto(tongGiaTri, vinhDanh));
}
@GetMapping("/lich-su/all")
public ResponseEntity<List<DoiTien>> getAllLichSu(@RequestParam(required = false) String loai) {
    // Gọi trực tiếp service để xử lý logic tìm kiếm toàn bộ
    return ResponseEntity.ok(doiTienService.getAllLichSu(loai));
}
@GetMapping("/lich-su")
public ResponseEntity<?> getLichSu(
        @RequestHeader("Authorization") String authHeader,
        @RequestParam(required = false) String loai
) {
    try {
        // 1. Lấy UID từ Tokena
        String uid = getUidFromHeader(authHeader);

        // 2. Gọi Service xử lý nghiệp vụ
        List<DoiTien> result = doiTienService.getLichSu(uid, loai);

        // 3. Trả về dữ liệu
        return ResponseEntity.ok(result);

    } catch (Exception e) {
        return ResponseEntity.status(401).body("Xác thực thất bại");
    }
}
}