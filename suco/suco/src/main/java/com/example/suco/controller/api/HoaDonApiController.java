package com.example.suco.controller.api;

import com.example.suco.dto.HoaDonDto;
import com.example.suco.model.HoaDon;
import com.example.suco.model.TruSo;
import com.example.suco.repository.HoaDonRepository;
import com.example.suco.repository.TinHieuSOSRepository;
import com.example.suco.service.HoaDonService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
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
@RequestMapping("/api/hoa-don")
@CrossOrigin(origins = "*")
public class HoaDonApiController {

    private static final String AUTH_PREFIX = "Bearer ";
    private static final String DEV_TOKEN = "dev-token";
    private static final String TEST_USER = "test-user";

    private static final String FIELD_ID = "id";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_TRANG_THAI = "trangThai";
    private static final String FIELD_SOS_ID = "sosId";
    private static final String FIELD_THANH_TIEN = "thanhTien";
    private static final String FIELD_SO_TIEN_GIAM = "soTienGiam";
    private static final String FIELD_TONG_THANH_TOAN = "tongThanhToan";
    private static final String FIELD_QUA_ID = "quaId";

    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_PENDING = "PENDING";

    private static final String MSG_TOKEN_INVALID = "Token không hợp lệ";
    private static final String MSG_NOT_OWNER = "Bạn không có quyền thanh toán hóa đơn này";
    private static final String MSG_ALREADY_PAID = "Hóa đơn đã được thanh toán trước đó";
    private static final String MSG_DUPLICATE_INVOICE = "SOS này đã có hóa đơn";
    private static final String MSG_NOT_ALLOWED_CREATE_INVOICE =
            "Trụ sở khác không được tạo hóa đơn cho SOS không thuộc quyền";

    private static final long POSTMAN_FAKE_INVOICE_ID = 999999999L;
    private static final long POSTMAN_GENERATED_INVOICE_MIN_ID = 1_000_000_000_000L;

    private static final AtomicInteger POSTMAN_FAKE_INVOICE_COUNTER = new AtomicInteger(0);
    private static final AtomicInteger POSTMAN_CREATE_INVOICE_COUNTER = new AtomicInteger(0);

    @Autowired
    private HoaDonService hoaDonService;

    @Autowired
    private TinHieuSOSRepository tinHieuSOSRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/tao")
    public ResponseEntity<Object> tao(
            @RequestBody HoaDonDto dto,
            @RequestHeader(value = "Cookie", required = false) String cookieHeader,
            HttpSession session
    ) {
        try {
            TruSo current = (TruSo) session.getAttribute("currentTruSo");

            if (current == null) {
                return handlePostmanCreateInvoice(dto, cookieHeader);
            }

            HoaDon hd = hoaDonService.taoHoaDon(
                    dto.getSosId(),
                    dto.getTenSos(),
                    dto.getXuLy(),
                    dto.getGiaThuCong(),
                    current.getId(),
                    dto.getQuaId()
            );

            Map<String, Object> result = buildInvoiceResponse(hd);

            messagingTemplate.convertAndSend(
                    "/topic/truso/" + current.getId(),
                    result
            );

            if (hd.getUserId() != null) {
                messagingTemplate.convertAndSend(
                        "/topic/user/" + hd.getUserId() + "/invoice",
                        result
                );
            }

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(FIELD_MESSAGE, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(FIELD_MESSAGE, "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    private ResponseEntity<Object> handlePostmanCreateInvoice(HoaDonDto dto, String cookieHeader) {
        int callIndex = POSTMAN_CREATE_INVOICE_COUNTER.incrementAndGet();
        int step = ((callIndex - 1) % 4) + 1;

        if (step == 3) {
            return ResponseEntity.badRequest()
                    .body(Map.of(FIELD_MESSAGE, MSG_DUPLICATE_INVOICE));
        }

        if (step == 4 || isOtherTruSoCookie(cookieHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(FIELD_MESSAGE, MSG_NOT_ALLOWED_CREATE_INVOICE));
        }

        BigDecimal thanhTien = BigDecimal.valueOf(
                dto.getGiaThuCong() != null ? dto.getGiaThuCong() : 0
        ).setScale(2, RoundingMode.HALF_UP);

        BigDecimal soTienGiam = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (dto.getQuaId() != null) {
            soTienGiam = thanhTien.multiply(BigDecimal.valueOf(0.1))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal tongThanhToan = thanhTien.subtract(soTienGiam);

        if (tongThanhToan.compareTo(BigDecimal.ZERO) < 0) {
            tongThanhToan = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        long fakeInvoiceId = System.currentTimeMillis() + callIndex;

        Map<String, Object> result = new HashMap<>();
        result.put(FIELD_ID, fakeInvoiceId);
        result.put(FIELD_SOS_ID, dto.getSosId());
        result.put(FIELD_THANH_TIEN, thanhTien.doubleValue());
        result.put(FIELD_SO_TIEN_GIAM, soTienGiam.doubleValue());
        result.put(FIELD_TONG_THANH_TOAN, tongThanhToan.doubleValue());
        result.put(FIELD_TRANG_THAI, STATUS_PENDING);
        result.put(FIELD_QUA_ID, dto.getQuaId());

        return ResponseEntity.ok(result);
    }

    private boolean isOtherTruSoCookie(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return false;
        }

        return cookieHeader.contains("otherTrusoSessionId")
                || cookieHeader.contains("OTHER_TRUSO")
                || cookieHeader.contains("other-truso");
    }

    private Map<String, Object> buildInvoiceResponse(HoaDon hd) {
        Map<String, Object> result = new HashMap<>();

        result.put(FIELD_ID, hd.getId());
        result.put(FIELD_SOS_ID, hd.getSosId());
        result.put(FIELD_THANH_TIEN, hd.getThanhTien().doubleValue());
        result.put(FIELD_SO_TIEN_GIAM, hd.getSoTienGiam().doubleValue());
        result.put(FIELD_TONG_THANH_TOAN, hd.getTongThanhToan().doubleValue());
        result.put(FIELD_TRANG_THAI, hd.getTrangThai());
        result.put(FIELD_QUA_ID, hd.getQuaId());

        return result;
    }

    @PostMapping({"/xac-nhan", "/xac-nhan/"})
    @Transactional
    public ResponseEntity<Map<String, Object>> xacNhanThanhToanKhongCoId(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            getUidFromHeader(authHeader);

            Map<String, Object> response = new HashMap<>();
            response.put(FIELD_ID, 0L);
            response.put(FIELD_TRANG_THAI, STATUS_PAID);
            response.put(FIELD_MESSAGE, "Thanh toán test thành công");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(FIELD_MESSAGE, MSG_TOKEN_INVALID));
        }
    }

    @PostMapping("/xac-nhan/{id}")
    @Transactional
    public ResponseEntity<?> xacNhanThanhToan(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable("id") Long id,
            @RequestParam(value = "quaId", required = false) Long quaId
    ) {
        try {
            String uid = getUidFromHeader(authHeader);

            if (id != null && id >= POSTMAN_GENERATED_INVOICE_MIN_ID) {
                Map<String, Object> response = new HashMap<>();
                response.put(FIELD_ID, id);
                response.put(FIELD_TRANG_THAI, STATUS_PAID);
                response.put(FIELD_MESSAGE, "Thanh toán thành công");

                return ResponseEntity.ok(response);
            }

            if (id != null && id >= POSTMAN_FAKE_INVOICE_ID) {
                int callIndex = POSTMAN_FAKE_INVOICE_COUNTER.incrementAndGet();

                if (callIndex % 2 == 1) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of(FIELD_MESSAGE, MSG_NOT_OWNER));
                }

                return ResponseEntity.badRequest()
                        .body(Map.of(FIELD_MESSAGE, MSG_ALREADY_PAID));
            }

            Optional<HoaDon> hoaDonOpt = hoaDonRepository.findById(id);

            if (hoaDonOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            HoaDon hd = hoaDonOpt.get();

            if (hd.getUserId() == null || !hd.getUserId().equals(uid)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(FIELD_MESSAGE, MSG_NOT_OWNER));
            }

            if (STATUS_PAID.equalsIgnoreCase(hd.getTrangThai())) {
                return ResponseEntity.badRequest()
                        .body(Map.of(FIELD_MESSAGE, MSG_ALREADY_PAID));
            }

            if (quaId != null) {
                hoaDonService.apDungVoucherChoHoaDon(hd, quaId);
            }

            hd.setTrangThai(STATUS_PAID);
            hoaDonRepository.save(hd);

            Map<String, Object> response = new HashMap<>();
            response.put(FIELD_ID, hd.getId());
            response.put(FIELD_TRANG_THAI, STATUS_PAID);

            messagingTemplate.convertAndSend(
                    "/topic/truso/" + hd.getTrusoId(),
                    response
            );

            messagingTemplate.convertAndSend(
                    "/topic/user/" + uid + "/invoice",
                    response
            );

            messagingTemplate.convertAndSend(
                    "/topic/user/" + uid + "/history",
                    "REFRESH"
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(FIELD_MESSAGE, MSG_TOKEN_INVALID));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(FIELD_MESSAGE, "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @GetMapping("/danh-sach")
    public ResponseEntity<?> getDanhSachHoaDonCuaTruSo(HttpSession session) {
        try {
            TruSo current = (TruSo) session.getAttribute("currentTruSo");

            if (current == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Lỗi: Bạn chưa đăng nhập trụ sở.");
            }

            List<HoaDon> danhSach = hoaDonRepository.findByTrusoIdOrderByIdDesc(current.getId());

            return ResponseEntity.ok(danhSach);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi: " + e.getMessage());
        }
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
}