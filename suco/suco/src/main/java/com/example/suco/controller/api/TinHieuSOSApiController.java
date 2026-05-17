        package com.example.suco.controller.api;

        import com.example.suco.dto.TinHieuSOSRequestDTO;
        import com.example.suco.model.TinHieuSOS;
        import com.example.suco.model.TruSo;
        import com.example.suco.repository.MuaGoiRepository;
        import com.example.suco.repository.TinHieuSOSRepository;
        import com.example.suco.service.DieuPhoiSOSService;
        import com.example.suco.service.DieuPhoiSOSService.ThongTinDieuPhoi;
        import com.example.suco.service.TinHieuSOSService;
        import com.google.firebase.auth.FirebaseAuth;
        import com.google.firebase.auth.FirebaseToken;

        import jakarta.servlet.http.HttpSession;

        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.messaging.simp.SimpMessagingTemplate;
        import org.springframework.http.ResponseEntity;
        import org.springframework.web.bind.annotation.*;
        import java.util.*;
        import java.util.stream.Collectors;

        @RestController
        @RequestMapping("/api/tin-hieu-sos")
        @CrossOrigin(origins = "*")
        public class TinHieuSOSApiController {

            @Autowired
            private com.example.suco.util.GeocodingUtil geocodingUtil;

            @Autowired
            private TinHieuSOSService tinHieuSOSService;

            @Autowired
            private SimpMessagingTemplate messagingTemplate;
            
            @Autowired 
            private TinHieuSOSRepository tinHieuSOSRepository;

            @Autowired
            private DieuPhoiSOSService dieuPhoiService;

            @Autowired
        private MuaGoiRepository muaGoiRepository; 

        @Autowired
    private com.example.suco.repository.HoaDonRepository hoaDonRepository;

@PostMapping("/submit")
public ResponseEntity<?> tiepNhanTinHieu(
    @RequestHeader("Authorization") String authHeader,
    @RequestBody TinHieuSOSRequestDTO dto
) {
    try {
        String token = authHeader.replace("Bearer ", "");
        String uid;
        // --- CƠ CHẾ BYPASS CHO DEV ---
        if ("dev-token".equals(token)) {
            uid = "test-user"; // ID này phải tồn tại trong bảng users của bạn để không lỗi Foreign Key
        } else {
            // Luồng thật cho App
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            uid = decodedToken.getUid();
        }
        // -----------------------------

        Map<String, Object> ketQua = tinHieuSOSService.xuLyTinHieuSOS(uid, dto);

        TinHieuSOS sosDaLuu = (TinHieuSOS) ketQua.get("sosData");
        if (sosDaLuu != null) {
            messagingTemplate.convertAndSend("/topic/admin", sosDaLuu);
        }

        return ResponseEntity.ok(sosDaLuu);

    } catch (Exception e) {
        return ResponseEntity.status(401).body("Xác thực thất bại: " + e.getMessage());
    }
}
    @GetMapping("/active")
public ResponseEntity<?> getSosActive(
    @RequestParam(required = false) String status, // Thêm dòng này để nhận filter từ Postman
    HttpSession session
) {
    TruSo current = (TruSo) session.getAttribute("currentTruSo");
    if (current == null) return ResponseEntity.status(401).body("Chưa đăng nhập");

    // 1. Lấy tất cả danh sách đang hoạt động (Chờ + Đang xử lý)
    List<TinHieuSOS> list = tinHieuSOSRepository.findActiveByTruSo(current.getId());

    // 2. Nếu Postman có truyền ?status=..., thì thực hiện lọc
    if (status != null && !status.isEmpty()) {
        list = list.stream()
                   .filter(sos -> sos.getTrangThai().equalsIgnoreCase(status))
                   .collect(Collectors.toList());
    }

    // 3. Set VIP (giữ nguyên logic cũ của bạn)
    for (TinHieuSOS sos : list) {
        boolean laVip = muaGoiRepository.findByUserId(sos.getUserId())
            .stream()
            .anyMatch(mg -> "ACTIVE".equalsIgnoreCase(mg.getTrangThai()));
        sos.setIsVip(laVip);
    }

    return ResponseEntity.ok(list);
}

    @PostMapping("/cap-nhat-trang-thai/{id}")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam("status") String status,
            HttpSession session
    ) {
        TruSo current = (TruSo) session.getAttribute("currentTruSo");
        if (current == null) {
            return ResponseEntity.status(401).body("Chưa đăng nhập");
        }

        // ✅ FIX 1: clean status triệt để
        if (status != null) {
            status = status.split(",")[0].trim();
        }

        Optional<TinHieuSOS> sosOpt = tinHieuSOSRepository.findById(id);

        if (sosOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TinHieuSOS sos = sosOpt.get();
        String currentStatus = sos.getTrangThai();

// ❌ CHẶN nhảy cóc trạng thái
if (!isValidTransition(currentStatus, status)) {
    return ResponseEntity.badRequest().body(
        Map.of("error", "Chuyển trạng thái không hợp lệ từ " + currentStatus + " sang " + status)
    );
}
if ("HOAN_THANH".equals(status) && sos.getIdTruSoTiepNhan() == null) {
    return ResponseEntity.badRequest().body(
        Map.of("error", "Chưa có trụ sở tiếp nhận nên không thể hoàn thành")
    );
}
if ("HOAN_THANH".equals(currentStatus) || "HUY_BO".equals(currentStatus)) {
    return ResponseEntity.badRequest().body(
        Map.of("error", "SOS đã kết thúc, không thể cập nhật")
    );
}

        // =========================
        // XỬ LÝ DANG_XU_LY
        // =========================
        if ("DANG_XU_LY".equals(status)) {

            Optional<ThongTinDieuPhoi> dpOpt = dieuPhoiService.layThongTinDieuPhoi(id);

            // ❌ Không thuộc danh sách điều phối
            if (dpOpt.isEmpty() ||
                !dpOpt.get().getDanhSachIdTruSo().contains(current.getId())) {

                return ResponseEntity.badRequest().body(
                    Map.of("error", "SOS này không thuộc về trụ sở của bạn")
                );
            }

            sos.setIdTruSoTiepNhan(current.getId());

            // đánh dấu đã nhận
            dieuPhoiService.danhDauDaTiepNhan(id, current.getId());
        }
        // CHỈ check quyền với HOAN_THANH
if ("HOAN_THANH".equals(status)) {

    if (sos.getIdTruSoTiepNhan() == null ||
        !sos.getIdTruSoTiepNhan().equals(current.getId())) {

        return ResponseEntity.status(403).body(
            Map.of("error", "Chỉ trụ sở tiếp nhận mới được hoàn thành SOS")
        );
    }
}

        // =========================
        // SET STATUS
        // =========================
        sos.setTrangThai(status);

        // =========================
        // HỦY ĐIỀU PHỐI nếu kết thúc
        // =========================
        if ("HOAN_THANH".equals(status) || "HUY_BO".equals(status)) {
            dieuPhoiService.huyDieuPhoi(id);
        }

        tinHieuSOSRepository.save(sos);

        // =========================
        // REALTIME
        // =========================
        Long targetTruSo = sos.getIdTruSoTiepNhan() != null
                ? sos.getIdTruSoTiepNhan()
                : current.getId();

        messagingTemplate.convertAndSend(
            "/topic/truso/" + targetTruSo,
            sos
        );

        // =========================
        // RESPONSE
        // =========================
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật thành công");
        response.put("status", status);

        return ResponseEntity.ok(response);
    }
            private String layThongDiepTrangThai(String trangThai) {
                switch (trangThai) {
                    case "DANG_XU_LY": return "Lực lượng cứu hộ đang đến!";
                    case "HOAN_THANH": return "Hỗ trợ đã hoàn tất.";
                    default: return "Yêu cầu đang được xử lý.";
                }
            }
        @GetMapping("/history")
    public ResponseEntity<?> getSosHistory(HttpSession session) {
        TruSo current = (TruSo) session.getAttribute("currentTruSo");

        if (current == null) {
            return ResponseEntity.status(401).body("Chưa đăng nhập");
        }

        List<TinHieuSOS> list = tinHieuSOSRepository.findHistoryByTruSo(current.getId());
        return ResponseEntity.ok(list);
    }
     @PostMapping("/cancel/{id}")
public ResponseEntity<?> cancelSOS(
    @RequestHeader("Authorization") String authHeader, 
    @PathVariable Long id
) {
    try {
       String token = authHeader.replace("Bearer ", "");
        String currentUid;

        // --- THÊM CƠ CHẾ BYPASS Ở ĐÂY ---
        if ("dev-token".equals(token)) {
            currentUid = "test-user"; 
        } else {
            // Luồng thật cho App Android
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            currentUid = decodedToken.getUid();
        }
        // ------------------------------

        return tinHieuSOSRepository.findById(id).map(sos -> {
            // 2. KIỂM TRA CHÍNH CHỦ: UID trong DB phải khớp với UID từ Token
            if (!sos.getUserId().equals(currentUid)) {
                return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền hủy yêu cầu này."));
            }

            if ("CHO_XU_LY".equals(sos.getTrangThai())) {
                sos.setTrangThai("HUY_BO");
                tinHieuSOSRepository.save(sos);
                
                dieuPhoiService.huyDieuPhoi(id);
                
                // --- REALTIME (Giữ nguyên logic của bạn) ---
                if (sos.getIdTruSoDeXuat() != null) {
                    messagingTemplate.convertAndSend("/topic/tru-so/" + sos.getIdTruSoDeXuat(), sos);
                }
                if (sos.getIdTruSoTiepNhan() != null) {
                    messagingTemplate.convertAndSend("/topic/tru-so/" + sos.getIdTruSoTiepNhan(), sos);
                }
                messagingTemplate.convertAndSend("/topic/admin", sos);
                messagingTemplate.convertAndSend("/topic/user/" + sos.getUserId() + "/sos-status", 
                    Map.of("idSOS", id, "trangThai", "HUY_BO", "message", "Bạn đã hủy yêu cầu cứu hộ"));
                messagingTemplate.convertAndSend("/topic/user/" + sos.getUserId() + "/history", "REFRESH");

                return ResponseEntity.ok(Map.of("message", "Đã hủy yêu cầu SOS thành công"));
            }
            return ResponseEntity.badRequest().body(Map.of("message", "Không thể hủy vì yêu cầu đang được xử lý hoặc đã kết thúc."));
        }).orElse(ResponseEntity.notFound().build());

    } catch (Exception e) {
        return ResponseEntity.status(401).body("Xác thực thất bại");
    }
}
            /**
             * Từ chối tiếp nhận SOS - chuyển tiếp ngay lập tức cho trụ sở tiếp theo.
             */
            @PostMapping("/tu-choi/{id}")
            public ResponseEntity<?> tuChoiTiepNhan(
                    @PathVariable Long id,
                    @RequestParam Long idTruSo) {
                
                boolean conTruSoTiepTheo = dieuPhoiService.tuChoiTiepNhan(id, idTruSo);
                
                if (conTruSoTiepTheo) {
                    return ResponseEntity.ok(Map.of(
                        "message", "Đã chuyển tiếp cho trụ sở tiếp theo",
                        "ketQua", "CHUYEN_TIEP_THANH_CONG"
                    ));
                } else {
                    return ResponseEntity.ok(Map.of(
                        "message", "Đã hết trụ sở trong danh sách hoặc không tìm thấy điều phối",
                        "ketQua", "HET_TRU_SO"
                    ));
                }
            }

            /**
             * Lấy thông tin điều phối của một SOS.
             * Trả về danh sách trụ sở theo thứ tự khoảng cách và thời gian còn lại.
             */
            @GetMapping("/dieu-phoi/{idSos}")
            public ResponseEntity<?> layThongTinDieuPhoi(@PathVariable Long idSos) {
                return dieuPhoiService.layThongTinDieuPhoi(idSos)
                    .map(dieuPhoi -> {
                        Map<String, Object> ketQua = new HashMap<>();
                        ketQua.put("idSos", dieuPhoi.getIdSos());
                        ketQua.put("trangThaiDieuPhoi", dieuPhoi.getTrangThaiDieuPhoi());
                        ketQua.put("chiMucTruSoHienTai", dieuPhoi.getChiMucTruSoHienTai());
                        ketQua.put("danhSachIdTruSo", dieuPhoi.getDanhSachIdTruSo());
                        ketQua.put("danhSachKhoangCach", dieuPhoi.getDanhSachKhoangCach());
                        ketQua.put("thoiGianGuiTinCuoi", dieuPhoi.getThoiGianGuiTinCuoi());
                        
                        // Tính thời gian còn lại (giây)
                        long giayDaQua = java.time.Duration.between(
                            dieuPhoi.getThoiGianGuiTinCuoi(), 
                            java.time.LocalDateTime.now()
                        ).getSeconds();
                        long thoiGianConLai = Math.max(0, 60 - giayDaQua);
                        ketQua.put("thoiGianConLai", thoiGianConLai);
                        
                        return ResponseEntity.ok(ketQua);
                    })
                    .orElse(ResponseEntity.notFound().build());
            }
            private boolean isValidTransition(String current, String next) {
    switch (current) {
        case "CHO_XU_LY":
            return "DANG_XU_LY".equals(next) || "HUY_BO".equals(next);

        case "DANG_XU_LY":
            return "HOAN_THANH".equals(next); // ❌ KHÔNG cho HUY

        default:
            return false;
    }
}

        }

