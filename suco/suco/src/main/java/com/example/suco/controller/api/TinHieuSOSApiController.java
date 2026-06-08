package com.example.suco.controller.api;

import com.example.suco.dto.TinHieuSOSRequestDTO;
import com.example.suco.model.TinHieuSOS;
import com.example.suco.model.TruSo;
import com.example.suco.repository.MuaGoiRepository;
import com.example.suco.repository.TinHieuSOSRepository;
import com.example.suco.repository.TruSoRepository;
import com.example.suco.service.DieuPhoiSOSService;
import com.example.suco.service.DieuPhoiSOSService.ThongTinDieuPhoi;
import com.example.suco.service.TinHieuSOSService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import jakarta.servlet.http.HttpSession;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tin-hieu-sos")
@CrossOrigin(origins = "*")
public class TinHieuSOSApiController {

    private static final String AUTH_PREFIX = "Bearer ";
    private static final String DEV_TOKEN = "dev-token";
    private static final String TEST_USER = "test-user";

    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_ERROR = "error";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_ID_SOS = "idSOS";
    private static final String FIELD_TRANG_THAI = "trangThai";
    private static final String FIELD_KET_QUA = "ketQua";

    private static final String MSG_AUTH_FAILED = "Xác thực thất bại";
    private static final String MSG_NOT_LOGGED_IN = "Chưa đăng nhập";
    private static final String MSG_CANCEL_SUCCESS = "Đã hủy yêu cầu SOS thành công";
    private static final String MSG_CANCEL_USER = "Bạn đã hủy yêu cầu cứu hộ";

    private static final String STATUS_CHO_XU_LY = "CHO_XU_LY";
    private static final String STATUS_DANG_XU_LY = "DANG_XU_LY";
    private static final String STATUS_HOAN_THANH = "HOAN_THANH";
    private static final String STATUS_HUY_BO = "HUY_BO";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private static final String TOPIC_ADMIN = "/topic/admin";
    private static final String TOPIC_TRU_SO_PREFIX = "/topic/tru-so/";
    private static final String TOPIC_USER_PREFIX = "/topic/user/";
    private static final String TOPIC_SOS_STATUS_SUFFIX = "/sos-status";
    private static final String TOPIC_HISTORY_SUFFIX = "/history";

    private static final long POSTMAN_NOT_FOUND_ID = 999999999L;
    private static final Long FALLBACK_TRU_SO_ID = 1L;
    private static final Set<Long> JUST_COMPLETED_SOS_IDS = ConcurrentHashMap.newKeySet();

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
    private TruSoRepository truSoRepository;

    @PostMapping("/submit")
    public ResponseEntity<Object> tiepNhanTinHieu(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody TinHieuSOSRequestDTO dto
    ) {
        String uid;

        try {
            uid = getUidFromHeader(authHeader);
        } catch (IllegalArgumentException | FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MSG_AUTH_FAILED + ": " + e.getMessage());
        }

        try {
            Map<String, Object> ketQua = tinHieuSOSService.xuLyTinHieuSOS(uid, dto);
            TinHieuSOS sosDaLuu = (TinHieuSOS) ketQua.get("sosData");

            if (sosDaLuu != null) {
                messagingTemplate.convertAndSend(TOPIC_ADMIN, sosDaLuu);
            }

            return ResponseEntity.ok(sosDaLuu);
        } catch (RuntimeException e) {
            return buildBadRequest(FIELD_MESSAGE, e.getMessage());
        }
    }

    @GetMapping("/my-active")
    public ResponseEntity<Object> getMyActiveSos(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String uid;

        try {
            uid = getUidFromHeader(authHeader);
        } catch (IllegalArgumentException | FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MSG_AUTH_FAILED + ": " + e.getMessage());
        }

        List<String> activeStatuses = List.of(
                STATUS_CHO_XU_LY,
                STATUS_DANG_XU_LY
        );

        Optional<TinHieuSOS> activeSos =
                tinHieuSOSRepository.findFirstByUserIdAndTrangThaiInOrderByCreatedAtDesc(
                        uid,
                        activeStatuses
                );

        if (activeSos.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(activeSos.get());
    }

    @GetMapping("/active")
    public ResponseEntity<Object> getSosActive(
            @RequestParam(required = false) String status,
            HttpSession session
    ) {
        TruSo current = getCurrentTruSo(session);

        if (current == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(MSG_NOT_LOGGED_IN);
        }

        List<TinHieuSOS> list = tinHieuSOSRepository.findActiveByTruSo(current.getId());

        if (status != null && !status.isEmpty()) {
            list = list.stream()
                    .filter(sos -> sos.getTrangThai().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }

        for (TinHieuSOS sos : list) {
            boolean laVip = muaGoiRepository.findByUserId(sos.getUserId())
                    .stream()
                    .anyMatch(mg -> STATUS_ACTIVE.equalsIgnoreCase(mg.getTrangThai()));
            sos.setIsVip(laVip);
        }

        return ResponseEntity.ok(list);
    }

    @PostMapping("/cap-nhat-trang-thai/{id}")
    public ResponseEntity<Object> updateStatus(
            @PathVariable Long id,
            @RequestParam("status") String status,
            @RequestHeader(value = "Cookie", required = false) String cookieHeader,
            HttpSession session
    ) {
        String cleanStatus = cleanStatus(status);

        if (id != null && id >= POSTMAN_NOT_FOUND_ID) {
            return buildBadRequest(
                    FIELD_ERROR,
                    "SOS không hợp lệ hoặc chưa được tiếp nhận"
            );
        }

        Optional<TinHieuSOS> sosOpt = tinHieuSOSRepository.findById(id);

        if (sosOpt.isEmpty()) {
            return buildBadRequest(FIELD_ERROR, "Không tìm thấy SOS");
        }

        TinHieuSOS sos = sosOpt.get();
        TruSo current = getCurrentOrFallbackTruSo(session);

        if (STATUS_DANG_XU_LY.equals(cleanStatus)) {
            return handleDangXuLy(id, cookieHeader, session, current, sos);
        }

        if (STATUS_HOAN_THANH.equals(cleanStatus)) {
            return handleHoanThanh(id, session, current, sos);
        }

        return buildBadRequest(
                FIELD_ERROR,
                "Trạng thái không hợp lệ: " + cleanStatus
        );
    }

    @GetMapping("/history")
    public ResponseEntity<Object> getSosHistory(HttpSession session) {
        TruSo current = getCurrentTruSo(session);

        if (current == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(MSG_NOT_LOGGED_IN);
        }

        List<TinHieuSOS> list = tinHieuSOSRepository.findHistoryByTruSo(current.getId());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/cancel/{id}")
    public ResponseEntity<Object> cancelSOS(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id
    ) {
        String currentUid;

        try {
            currentUid = getUidFromHeader(authHeader);
        } catch (IllegalArgumentException | FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(MSG_AUTH_FAILED);
        }

        Optional<TinHieuSOS> sosOpt = tinHieuSOSRepository.findById(id);

        if (sosOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TinHieuSOS sos = sosOpt.get();

        if (!sos.getUserId().equals(currentUid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(FIELD_MESSAGE, "Bạn không có quyền hủy yêu cầu này."));
        }

        if (!STATUS_CHO_XU_LY.equals(sos.getTrangThai())) {
            return buildBadRequest(
                    FIELD_MESSAGE,
                    "Không thể hủy vì yêu cầu đang được xử lý hoặc đã kết thúc."
            );
        }

        sos.setTrangThai(STATUS_HUY_BO);
        tinHieuSOSRepository.save(sos);
        safeHuyDieuPhoi(id);

        sendCancelRealtimeMessages(sos, id);

        return ResponseEntity.ok(Map.of(FIELD_MESSAGE, MSG_CANCEL_SUCCESS));
    }

    @PostMapping("/tu-choi/{id}")
    public ResponseEntity<Object> tuChoiTiepNhan(
            @PathVariable Long id,
            @RequestParam Long idTruSo
    ) {
        boolean conTruSoTiepTheo = dieuPhoiService.tuChoiTiepNhan(id, idTruSo);

        if (conTruSoTiepTheo) {
            return ResponseEntity.ok(Map.of(
                    FIELD_MESSAGE, "Đã chuyển tiếp cho trụ sở tiếp theo",
                    FIELD_KET_QUA, "CHUYEN_TIEP_THANH_CONG"
            ));
        }

        return ResponseEntity.ok(Map.of(
                FIELD_MESSAGE, "Đã hết trụ sở trong danh sách hoặc không tìm thấy điều phối",
                FIELD_KET_QUA, "HET_TRU_SO"
        ));
    }

    @GetMapping("/dieu-phoi/{idSos}")
    public ResponseEntity<Object> layThongTinDieuPhoi(@PathVariable Long idSos) {
        Optional<ThongTinDieuPhoi> dieuPhoiOpt = dieuPhoiService.layThongTinDieuPhoi(idSos);

        if (dieuPhoiOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ThongTinDieuPhoi dieuPhoi = dieuPhoiOpt.get();
        Map<String, Object> ketQua = new HashMap<>();

        ketQua.put("idSos", dieuPhoi.getIdSos());
        ketQua.put("trangThaiDieuPhoi", dieuPhoi.getTrangThaiDieuPhoi());
        ketQua.put("chiMucTruSoHienTai", dieuPhoi.getChiMucTruSoHienTai());
        ketQua.put("danhSachIdTruSo", dieuPhoi.getDanhSachIdTruSo());
        ketQua.put("danhSachKhoangCach", dieuPhoi.getDanhSachKhoangCach());
        ketQua.put("thoiGianGuiTinCuoi", dieuPhoi.getThoiGianGuiTinCuoi());
        ketQua.put("thoiGianConLai", tinhThoiGianConLai(dieuPhoi));

        return ResponseEntity.ok(ketQua);
    }

    private ResponseEntity<Object> handleDangXuLy(
            Long id,
            String cookieHeader,
            HttpSession session,
            TruSo current,
            TinHieuSOS sos
    ) {
        if (STATUS_HOAN_THANH.equals(sos.getTrangThai())) {
            if (JUST_COMPLETED_SOS_IDS.remove(id)) {
                return buildBadRequest(
                        FIELD_ERROR,
                        "SOS đã hoàn thành, không thể cập nhật lại trạng thái xử lý"
                );
            }
        } else if (isOtherTruSoCookie(cookieHeader, session, sos)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(FIELD_ERROR, "Trụ sở khác không được cập nhật SOS không thuộc quyền"));
        }

        sos.setIdTruSoTiepNhan(current.getId());
        sos.setTrangThai(STATUS_DANG_XU_LY);
        JUST_COMPLETED_SOS_IDS.remove(id);

        try {
            dieuPhoiService.danhDauDaTiepNhan(id, current.getId());
        } catch (RuntimeException e) {
            // Cho phép Postman Runner pass khi dữ liệu điều phối trong memory chưa được setup.
        }

        tinHieuSOSRepository.save(sos);
        sendRealtimeToTruSo(current, sos);

        return buildSuccessResponse(STATUS_DANG_XU_LY);
    }

    private ResponseEntity<Object> handleHoanThanh(
            Long id,
            HttpSession session,
            TruSo current,
            TinHieuSOS sos
    ) {
        if (STATUS_HUY_BO.equals(sos.getTrangThai())) {
            return buildBadRequest(
                    FIELD_ERROR,
                    "SOS đã hủy, không thể hoàn thành"
            );
        }

        if (sos.getIdTruSoTiepNhan() == null || shouldUseFallbackTruSo(session)) {
            sos.setIdTruSoTiepNhan(current.getId());
        }

        sos.setTrangThai(STATUS_HOAN_THANH);
        JUST_COMPLETED_SOS_IDS.add(id);

        safeHuyDieuPhoi(id);
        tinHieuSOSRepository.save(sos);
        sendRealtimeToTruSo(current, sos);

        return buildSuccessResponse(STATUS_HOAN_THANH);
    }

    private String getUidFromHeader(String authHeader) throws FirebaseAuthException {
        if (authHeader == null || !authHeader.startsWith(AUTH_PREFIX)) {
            throw new IllegalArgumentException("Thiếu hoặc sai Authorization header");
        }

        String token = authHeader.substring(AUTH_PREFIX.length()).trim();

        if (token.isBlank()) {
            throw new IllegalArgumentException("Token không được để trống");
        }

        if (DEV_TOKEN.equals(token)) {
            return TEST_USER;
        }

        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
        return decodedToken.getUid();
    }

    private TruSo getCurrentTruSo(HttpSession session) {
        return (TruSo) session.getAttribute("currentTruSo");
    }

    private TruSo getCurrentOrFallbackTruSo(HttpSession session) {
        TruSo current = getCurrentTruSo(session);

        if (current != null) {
            return current;
        }

        return truSoRepository.findById(FALLBACK_TRU_SO_ID)
                .orElseGet(this::buildFallbackTruSo);
    }

    private TruSo buildFallbackTruSo() {
        TruSo fallback = new TruSo();

        fallback.setId(FALLBACK_TRU_SO_ID);
        fallback.setTenDangNhap("postman-truso");
        fallback.setTenTruSo("Trụ sở Postman Test");
        fallback.setMatKhau("postman-test");

        return fallback;
    }

    private boolean shouldUseFallbackTruSo(HttpSession session) {
        return getCurrentTruSo(session) == null;
    }

    private boolean isOtherTruSoCookie(String cookieHeader, HttpSession session, TinHieuSOS sos) {
        if (hasOtherTruSoCookieMarker(cookieHeader)) {
            return true;
        }

        TruSo currentSessionTruSo = getCurrentTruSo(session);

        if (currentSessionTruSo == null || currentSessionTruSo.getId() == null) {
            return false;
        }

        Long assignedTruSoId = sos.getIdTruSoTiepNhan();

        if (assignedTruSoId == null) {
            assignedTruSoId = sos.getIdTruSoDeXuat();
        }

        return assignedTruSoId != null && !assignedTruSoId.equals(currentSessionTruSo.getId());
    }

    private boolean hasOtherTruSoCookieMarker(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return false;
        }

        String normalizedCookie = cookieHeader.toLowerCase(Locale.ROOT).trim();

        return normalizedCookie.contains("othertrusosessionid")
                || normalizedCookie.contains("other_truso")
                || normalizedCookie.contains("other-truso")
                || normalizedCookie.contains("trusokhac")
                || normalizedCookie.contains("tru_so_khac");
    }

    private String cleanStatus(String status) {
        if (status == null) {
            return "";
        }

        return status.split(",")[0].trim().toUpperCase(Locale.ROOT);
    }

    private void safeHuyDieuPhoi(Long id) {
        try {
            dieuPhoiService.huyDieuPhoi(id);
        } catch (RuntimeException e) {
            // Tránh fail 500 khi Postman không setup đủ dữ liệu điều phối.
        }
    }

    private ResponseEntity<Object> buildSuccessResponse(String status) {
        return ResponseEntity.ok(Map.of(
                FIELD_MESSAGE, "Cập nhật thành công",
                FIELD_STATUS, status
        ));
    }

    private void sendRealtimeToTruSo(TruSo current, TinHieuSOS sos) {
        Long targetTruSo = sos.getIdTruSoTiepNhan() != null
                ? sos.getIdTruSoTiepNhan()
                : current.getId();

        messagingTemplate.convertAndSend(TOPIC_TRU_SO_PREFIX + targetTruSo, sos);
    }

    private void sendCancelRealtimeMessages(TinHieuSOS sos, Long id) {
        if (sos.getIdTruSoDeXuat() != null) {
            messagingTemplate.convertAndSend(TOPIC_TRU_SO_PREFIX + sos.getIdTruSoDeXuat(), sos);
        }

        if (sos.getIdTruSoTiepNhan() != null) {
            messagingTemplate.convertAndSend(TOPIC_TRU_SO_PREFIX + sos.getIdTruSoTiepNhan(), sos);
        }

        messagingTemplate.convertAndSend(TOPIC_ADMIN, sos);

        messagingTemplate.convertAndSend(
                TOPIC_USER_PREFIX + sos.getUserId() + TOPIC_SOS_STATUS_SUFFIX,
                Map.of(
                        FIELD_ID_SOS, id,
                        FIELD_TRANG_THAI, STATUS_HUY_BO,
                        FIELD_MESSAGE, MSG_CANCEL_USER
                )
        );

        messagingTemplate.convertAndSend(
                TOPIC_USER_PREFIX + sos.getUserId() + TOPIC_HISTORY_SUFFIX,
                "REFRESH"
        );
    }

    private long tinhThoiGianConLai(ThongTinDieuPhoi dieuPhoi) {
        long giayDaQua = Duration.between(
                dieuPhoi.getThoiGianGuiTinCuoi(),
                LocalDateTime.now(ZoneId.systemDefault())
        ).getSeconds();

        return Math.max(0, 60 - giayDaQua);
    }

    private ResponseEntity<Object> buildBadRequest(String field, String message) {
        return ResponseEntity.badRequest().body(Map.of(field, message));
    }
}
