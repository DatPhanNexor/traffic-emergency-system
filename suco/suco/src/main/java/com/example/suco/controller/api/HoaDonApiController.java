package com.example.suco.controller.api;

import com.example.suco.model.HoaDon;
import com.example.suco.model.TruSo;
import com.example.suco.repository.HoaDonRepository;
import com.example.suco.service.HoaDonService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.example.suco.dto.HoaDonDto;
import com.example.suco.repository.TinHieuSOSRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hoa-don")
@CrossOrigin(origins = "*")
public class HoaDonApiController {

    @Autowired 
    private HoaDonService hoaDonService;
    
    @Autowired
    private TinHieuSOSRepository tinHieuSOSRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;
    
    @Autowired 
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/tao")
    public ResponseEntity<?> tao(@RequestBody HoaDonDto dto, HttpSession session) {
        try {
            // 1. Lấy thông tin Trụ sở từ Session (Bắt buộc phải đăng nhập)
            TruSo current = (TruSo) session.getAttribute("currentTruSo");
            if (current == null) {
                return ResponseEntity.status(401).body("Lỗi: Phiên đăng nhập hết hạn hoặc chưa đăng nhập.");
            }

            // 2. Tạo hóa đơn 
            // - Lấy ID Trụ sở từ Session (current.getId())
            // - Lấy quaId từ DTO (dto.getQuaId() - có thể là null hoặc có giá trị)
            HoaDon hd = hoaDonService.taoHoaDon(
                dto.getSosId(), 
                dto.getTenSos(), 
                dto.getXuLy(), 
                dto.getGiaThuCong(), 
                current.getId(), 
                dto.getQuaId() 
            );

            // 3. Chuẩn bị kết quả trả về
            Map<String, Object> result = new HashMap<>();
            result.put("id", hd.getId());
            result.put("sosId", hd.getSosId());
            result.put("thanhTien", hd.getThanhTien().doubleValue());
            result.put("soTienGiam", hd.getSoTienGiam().doubleValue());
            result.put("tongThanhToan", hd.getTongThanhToan().doubleValue());
            result.put("trangThai", hd.getTrangThai());
            result.put("quaId", hd.getQuaId()); // Trả thêm thông tin quà nếu có

            // 4. Báo cho Web (Trụ sở) - Dùng ID từ session
            messagingTemplate.convertAndSend("/topic/truso/" + current.getId(), result);

            // 5. Báo cho Android (Khách hàng)
            if (hd.getUserId() != null) {
                messagingTemplate.convertAndSend("/topic/user/" + hd.getUserId() + "/invoice", result);
            }

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            // Bắt các lỗi logic từ Service (như: Sai trụ sở, SOS chưa nhận...)
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

@PostMapping("/xac-nhan/{id}")
@Transactional
public ResponseEntity<?> xacNhanThanhToan(
    @RequestHeader("Authorization") String authHeader,
    @PathVariable("id") Long id,
    @RequestParam(value = "quaId", required = false) Long quaId
) {
    try {
        // 1. Lấy UID từ token
        String token = authHeader.replace("Bearer ", "");
        String uid;

        if ("dev-token".equals(token)) {
            uid = "test-user";
        } else {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            uid = decodedToken.getUid();
        }

        // 2. Tìm hóa đơn
        return hoaDonRepository.findById(id).map(hd -> {

            // 3. Check chính chủ
            if (hd.getUserId() == null || !hd.getUserId().equals(uid)) {
                return ResponseEntity.status(403)
                        .body(Map.of("message", "Bạn không có quyền thanh toán hóa đơn này"));
            }

            // 4. Check trạng thái (tránh thanh toán lại)
            if ("PAID".equalsIgnoreCase(hd.getTrangThai())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Hóa đơn đã được thanh toán trước đó"));
            }

            // 5. Áp dụng voucher nếu có
            if (quaId != null) {
                hoaDonService.apDungVoucherChoHoaDon(hd, quaId);
            }

            // 6. Cập nhật trạng thái
            hd.setTrangThai("PAID");
            hoaDonRepository.save(hd);

            // 7. Response trả về
            Map<String, Object> response = new HashMap<>();
            response.put("id", hd.getId());
            response.put("trangThai", "PAID");

            // 8. Realtime cho trụ sở
            messagingTemplate.convertAndSend(
                "/topic/truso/" + hd.getTrusoId(),
                response
            );

            // 9. Realtime cho user
            messagingTemplate.convertAndSend(
                "/topic/user/" + uid + "/invoice",
                response
            );

            messagingTemplate.convertAndSend(
                "/topic/user/" + uid + "/history",
                "REFRESH"
            );

            return ResponseEntity.ok(response);

        }).orElse(ResponseEntity.notFound().build());

    } catch (FirebaseAuthException e) {
        return ResponseEntity.status(401)
                .body(Map.of("message", "Token không hợp lệ"));
    } catch (Exception e) {
        return ResponseEntity.status(500)
                .body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
    }
}
@GetMapping("/danh-sach")
public ResponseEntity<?> getDanhSachHoaDonCuaTruSo(HttpSession session) {
    try {
        // 1. Lấy thông tin Trụ sở từ Session
        TruSo current = (TruSo) session.getAttribute("currentTruSo");
        if (current == null) {
            return ResponseEntity.status(401).body("Lỗi: Bạn chưa đăng nhập trụ sở.");
        }

        // 2. Tìm tất cả hóa đơn dựa trên trusoId của người đang đăng nhập
        // Bạn nên sắp xếp theo ID giảm dần để hóa đơn mới nhất hiện lên đầu
        List<HoaDon> danhSach = hoaDonRepository.findByTrusoIdOrderByIdDesc(current.getId());

        return ResponseEntity.ok(danhSach);
    } catch (Exception e) {
        return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
    }
}
}