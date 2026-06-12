package com.example.suco.controller.api;

import com.example.suco.dto.AiRejectResponse;
import com.example.suco.dto.SuCoMapDto;
import com.example.suco.model.BaoCaoSuCo;
import com.example.suco.model.LoaiSuCo;
import com.example.suco.model.User;
import com.example.suco.repository.BaoCaoSuCoRepository;
import com.example.suco.repository.LoaiSuCoRepository;
import com.example.suco.repository.UserRepository;
import com.example.suco.service.AiVerifyResult;
import com.example.suco.service.suco.baocao.user.UserBaoCaoService;
import com.example.suco.service.xacthuc.user.token.FirebaseService;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
public class IncidentApiController {

    @Autowired
    private BaoCaoSuCoRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoaiSuCoRepository loaiSuCoRepository;

    @Autowired
    private UserBaoCaoService userBaoCaoService;

    @Autowired
    private FirebaseService firebaseService;

    @PostMapping
    public ResponseEntity<AiRejectResponse> createIncident(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> payload
    ) {
        try {
            Double viDo = getDouble(payload, "viDo", "lat", "latitude");
            Double kinhDo = getDouble(payload, "kinhDo", "lng", "longitude");
            String hinhAnhUrl = getString(payload, "hinhAnhUrl", "imageUrl", "image", "hinhAnh", "photo", "base64");
            Long loaiId = getLong(payload, "loaiSuCoId", "loaiId", "typeId", "categoryId", "loaiSuCo");
            String loaiTen = getString(payload, "loaiSuCoTen", "loaiTen", "type", "category");
            Object loaiObj = payload != null ? payload.get("loaiSuCo") : null;

            if (loaiObj instanceof Map<?, ?> loaiMap) {
                if (loaiId == null) {
                    loaiId = getLongFromMap(loaiMap, "id", "loaiId", "typeId");
                }
                if (loaiTen == null) {
                    loaiTen = getStringFromMap(loaiMap, "ten", "name", "type");
                }
            }

            LoaiSuCo loaiSuCo = null;
            if (loaiId != null) {
                loaiSuCo = loaiSuCoRepository.findById(loaiId).orElse(null);
            }
            if (loaiSuCo == null && loaiTen != null) {
                loaiSuCo = loaiSuCoRepository.findByTen(loaiTen).orElse(null);
            }

            if (loaiSuCo == null) {
                LoaiSuCo created = new LoaiSuCo();
                if (loaiId != null) {
                    created.setId(loaiId);
                }
                created.setTen(loaiTen != null ? loaiTen : "Loai su co mac dinh");
                created.setIconUrl("");
                loaiSuCo = loaiSuCoRepository.save(created);
            }

            String uid = resolveUid(authHeader);

            User user = userRepository.findById(uid)
                    .orElseGet(() -> createTestUser(uid));

            BaoCaoSuCo report = new BaoCaoSuCo();
            report.setReporter(user);
            report.setLoaiSuCo(loaiSuCo);
            report.setViDo(viDo != null ? viDo : 0.0);
            report.setKinhDo(kinhDo != null ? kinhDo : 0.0);
            report.setMoTa(getString(payload, "moTa", "description", "note"));
            report.setHinhAnhUrl(hinhAnhUrl != null ? hinhAnhUrl : "mock-image");

            AiVerifyResult ai =
                    userBaoCaoService.submitReport(uid, report, null);

            if (!ai.isValid()) {
                String code = ai.getReason() != null
                        && ai.getReason().contains("trước đó")
                        ? "DUPLICATE"
                        : "AI_REJECTED";

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        new AiRejectResponse(
                                code,
                                ai.getReason(),
                                ai.getConfidence() != null ? ai.getConfidence() : 0,
                                ai.getDistance()
                        )
                );
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new AiRejectResponse(
                            "AI_APPROVED",
                            "Báo cáo sự cố thành công",
                            100,
                            ai.getDistance()
                    )
            );
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new AiRejectResponse(
                            "UNAUTHORIZED",
                            "Lỗi xác thực: " + e.getMessage(),
                            0,
                            null
                    )
            );
        }
    }

    @GetMapping
    public List<SuCoMapDto> getIncidents() {
        return reportRepository.findAllForMap();
    }

    @GetMapping("/maps")
    public List<SuCoMapDto> getIncidentsForMap() {
        return reportRepository.findAllForMap();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateIncidentStatus(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long idTruSo
    ) {
        if (!reportRepository.existsById(id)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Không tìm thấy sự cố"));
        }

        String resolvedStatus = status;
        if (resolvedStatus == null && body != null) {
            resolvedStatus = body.get("status");
            if (resolvedStatus == null) {
                resolvedStatus = body.get("trangThai");
            }
            if (resolvedStatus == null) {
                resolvedStatus = body.get("trangThaiXuLy");
            }
        }

        return userBaoCaoService.updateReportStatus(id, resolvedStatus, idTruSo);
    }

    private String resolveUid(String authHeader) throws FirebaseAuthException {
        if (authHeader == null || authHeader.isBlank()) {
            return "test-user";
        }

        return firebaseService.extractUid(authHeader);
    }

    private String getString(Map<String, Object> payload, String... keys) {
        if (payload == null) {
            return null;
        }

        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null) {
                String text = String.valueOf(value).trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }

        return null;
    }

    private Double getDouble(Map<String, Object> payload, String... keys) {
        if (payload == null) {
            return null;
        }

        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (value instanceof String text && !text.isBlank()) {
                try {
                    return Double.parseDouble(text.trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }

        return null;
    }

    private Long getLong(Map<String, Object> payload, String... keys) {
        if (payload == null) {
            return null;
        }

        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            if (value instanceof String text && !text.isBlank()) {
                try {
                    return Long.parseLong(text.trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }

        return null;
    }

    private String getStringFromMap(Map<?, ?> payload, String... keys) {
        if (payload == null) {
            return null;
        }

        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null) {
                String text = String.valueOf(value).trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }

        return null;
    }

    private Long getLongFromMap(Map<?, ?> payload, String... keys) {
        if (payload == null) {
            return null;
        }

        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            if (value instanceof String text && !text.isBlank()) {
                try {
                    return Long.parseLong(text.trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }

        return null;
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
