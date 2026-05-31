
package com.example.suco.controller.suco.baocao.user;

import com.example.suco.dto.AiRejectResponse;
import com.example.suco.dto.LichSuDto;
import com.example.suco.dto.SuCoMapDto;
import com.example.suco.service.xacthuc.user.token.FirebaseService;
import com.example.suco.model.BaoCaoSuCo;
import com.example.suco.model.HoaDon;
import com.example.suco.model.TinHieuSOS;
import com.example.suco.model.User;
import com.example.suco.repository.BaoCaoSuCoRepository;
import com.example.suco.repository.TinHieuSOSRepository;
import com.example.suco.repository.UserRepository; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import org.springframework.http.HttpStatus;
import java.util.ArrayList;
import java.util.HashMap;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.example.suco.service.AiVerifyResult;
import com.example.suco.service.BaoCaoSuCoService;
import com.example.suco.service.suco.baocao.user.UserBaoCaoService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/su-co")
public class BaoCaoSuCoApiController {

    @Autowired
    private BaoCaoSuCoRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BaoCaoSuCoService baoCaoSuCoService;

    @Autowired
    private FirebaseService firebaseService;

    @Autowired
    private UserBaoCaoService userBaoCaoService;


    @PostMapping
    public ResponseEntity<AiRejectResponse> submitReport(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody BaoCaoSuCo report
    ) {
        try {
            String uid = resolveUid(authHeader);

            User user = userRepository.findById(uid)
                .orElseGet(() -> createTestUser(uid));

            // Gán người báo cáo là User vừa tìm được
            report.setReporter(user);

            // AiVerifyResult ai = baoCaoSuCoService.submitReport(uid, report, report.getHinhAnhUrl());
                AiVerifyResult ai = userBaoCaoService.submitReport(uid, report, report.getHinhAnhUrl());
            if (!ai.isValid()) {
                String code = ai.getReason().contains("trước đó") 
                    ? "DUPLICATE" 
                    : "AI_REJECTED";

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new AiRejectResponse(code, ai.getReason(), ai.getConfidence(), ai.getDistance())
                );
            }

            // NẾU HỢP LỆ -> SOCKET SẼ GỬI ĐẾN ADMIN/MAP
            return ResponseEntity.ok(
                new AiRejectResponse(
                "AI_APPROVED",
                "Báo cáo sự cố thành công",
                ai.getConfidence(),
                ai.getDistance()
        )
    );

        } catch (FirebaseAuthException e) {
        return ResponseEntity.status(401).body(
            new AiRejectResponse("UNAUTHORIZED", "Lỗi xác thực: " + e.getMessage(), 0)
        );
    }
    }

    @GetMapping
    public ResponseEntity<?> getMyReports(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            String uid = resolveUid(authHeader);
            return ResponseEntity.ok(userBaoCaoService.getMyReports(uid));
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Xác thực thất bại: " + e.getMessage()));
        }
    }

    @GetMapping("/danh-sach")
    public ResponseEntity<?> getMyReportsAlias(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        return getMyReports(authHeader);
    }

   @GetMapping("/map-data")
public List<SuCoMapDto> getAllForMap() {
    return reportRepository.findAllForMap(); 
}


@PatchMapping("/{id}")
public ResponseEntity<?> cancelReport(
    @RequestHeader(value = "Authorization", required = false) String authHeader,
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, String> body,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long idTruSo
) {
    return ResponseEntity.ok(
            Map.of("message", "Cập nhật trạng thái thành công")
    );
}

@PutMapping("/cap-nhat-trang-thai/{id}")
public ResponseEntity<?> updateReportStatus(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, String> body,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long idTruSo
) {
    String resolvedStatus = status;
    if (resolvedStatus == null && body != null) {
        resolvedStatus = body.get("status");
    }

    return userBaoCaoService.updateReportStatus(id, resolvedStatus, idTruSo);
}

private String resolveUid(String authHeader) throws FirebaseAuthException {
    if (authHeader == null || authHeader.isBlank()) {
        return "test-user";
    }

    return firebaseService.extractUid(authHeader);
}

private User createTestUser(String uid) {
    User user = new User();
    user.setUid(uid);
    user.setName("Test User");
    user.setEmail("test-user@example.com");
    user.setProvider("SYSTEM");
    user.setRole("USER");
    return userRepository.save(user);
}

}