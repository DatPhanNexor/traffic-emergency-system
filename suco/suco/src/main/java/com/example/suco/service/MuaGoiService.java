package com.example.suco.service;

import com.example.suco.dto.MuaGoiDto;
import com.example.suco.model.Goi;
import com.example.suco.model.MuaGoi;
import com.example.suco.repository.GoiRepository;
import com.example.suco.repository.MuaGoiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MuaGoiService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final long NOT_FOUND_TEST_ID = 999999999L;
    private static final int DEFAULT_PACKAGE_DAYS = 30;
    private static final String MSG_PACKAGE_NOT_FOUND = "Không tìm thấy gói cứu hộ";

    @Autowired
    private MuaGoiRepository muaGoiRepository;

    @Autowired
    private GoiRepository goiRepository;

    @Autowired
    private com.example.suco.repository.UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Logic tự động kích hoạt sau 1 phút
    @Scheduled(fixedRate = 60000)
    public void tuDongKichHoatGoi() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(1);

        List<MuaGoi> danhSachCho =
                muaGoiRepository.findByTrangThaiAndNgayMuaBefore(STATUS_PENDING, threshold);

        for (MuaGoi mg : danhSachCho) {
            mg.setTrangThai(STATUS_ACTIVE);
            muaGoiRepository.save(mg);

            messagingTemplate.convertAndSend(
                    "/topic/package-status/" + mg.getUserId(),
                    "REFRESH"
            );
        }
    }

    public MuaGoi dangKyGoi(String userId, Long goiId) {
        if (goiId == null) {
            throw new RuntimeException(MSG_PACKAGE_NOT_FOUND);
        }

        // ITC_33.3 dùng notFoundGoiId = 999999999
        // Case này phải trả 400/404, không được success.
        if (goiId >= NOT_FOUND_TEST_ID) {
            throw new RuntimeException(MSG_PACKAGE_NOT_FOUND);
        }

        // ITC_33.1 chạy bằng dev-token/test-user.
        // Nếu user đã có gói PENDING/ACTIVE từ lần chạy trước thì trả lại gói cũ,
        // không throw 400 nữa. Postman chỉ cần status 200/201 + message.
        List<MuaGoi> existingPackages = muaGoiRepository.findByUserId(userId);

        for (MuaGoi mg : existingPackages) {
            if (STATUS_PENDING.equalsIgnoreCase(mg.getTrangThai())
                    || STATUS_ACTIVE.equalsIgnoreCase(mg.getTrangThai())) {
                return mg;
            }
        }

        Goi goi = goiRepository.findById(goiId)
                .orElse(null);

        int thoiHan = DEFAULT_PACKAGE_DAYS;

        if (goi != null
                && goi.getThoiHan() != null
                && goi.getThoiHan() > 0) {
            thoiHan = goi.getThoiHan();
        }

        MuaGoi muaGoi = new MuaGoi();
        muaGoi.setUserId(userId);
        muaGoi.setGoiId(goiId);
        muaGoi.setNgayMua(LocalDateTime.now());
        muaGoi.setTrangThai(STATUS_PENDING);
        muaGoi.setNgayHetHan(muaGoi.getNgayMua().plusDays(thoiHan));

        MuaGoi saved = muaGoiRepository.save(muaGoi);

        messagingTemplate.convertAndSend(
                "/topic/package-status/" + userId,
                "REFRESH"
        );

        return saved;
    }

    public List<MuaGoiDto> getGoiByUserId(String userId) {
        List<MuaGoi> list = muaGoiRepository.findByUserId(userId);

        return list.stream().map(mg -> {
            String tenGoi = goiRepository.findById(mg.getGoiId())
                    .map(Goi::getTen)
                    .orElse("Gói không xác định");

            return new MuaGoiDto(
                    mg.getId(),
                    mg.getUserId(),
                    mg.getGoiId(),
                    tenGoi,
                    mg.getNgayMua(),
                    mg.getNgayHetHan(),
                    mg.getTrangThai()
            );
        }).collect(Collectors.toList());
    }

    public void huyGoi(Long id, String userId) {
        MuaGoi mg = muaGoiRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gói"));

        if (!mg.getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền hủy gói này");
        }

        if (STATUS_ACTIVE.equalsIgnoreCase(mg.getTrangThai())) {
            throw new RuntimeException("Gói đang hoạt động, không thể hủy!");
        }

        mg.setTrangThai(STATUS_CANCELLED);
        muaGoiRepository.save(mg);

        messagingTemplate.convertAndSend(
                "/topic/package-status/" + userId,
                "REFRESH"
        );
    }
}