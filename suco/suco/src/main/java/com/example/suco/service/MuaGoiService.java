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

    @Autowired
    private MuaGoiRepository muaGoiRepository;
    
    @Autowired
    private GoiRepository goiRepository;

    @Autowired
    private com.example.suco.repository.UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate; // Thêm để bắn Socket

    // Logic tự động kích hoạt sau 1 phút
    @Scheduled(fixedRate = 60000) // 🔥 đổi 10s -> 1 phút
public void tuDongKichHoatGoi() {

    LocalDateTime threshold = LocalDateTime.now().minusMinutes(1);

    List<MuaGoi> danhSachCho =
            muaGoiRepository.findByTrangThaiAndNgayMuaBefore("PENDING", threshold);

    for (MuaGoi mg : danhSachCho) {
        mg.setTrangThai("ACTIVE");
        muaGoiRepository.save(mg);

        messagingTemplate.convertAndSend(
                "/topic/package-status/" + mg.getUserId(),
                "REFRESH"
        );
    }
}

    public MuaGoi dangKyGoi(String userId, Long goiId) {
        // 1. Tìm xem người dùng đã có gói nào chưa (Pending hoặc Active)
        List<MuaGoi> existingPackages = muaGoiRepository.findByUserId(userId);

        for (MuaGoi mg : existingPackages) {
            if ("PENDING".equals(mg.getTrangThai())) {
                // Gửi thông báo qua Socket ngay lập tức (tùy chọn)
                messagingTemplate.convertAndSend("/topic/package-status/" + userId, 
                    "Bạn đang có gói chờ kích hoạt. Vui lòng hủy gói cũ để đăng ký gói mới!");
                throw new RuntimeException("Bạn phải hủy gói đang chờ để mua gói này");
            }
            if ("ACTIVE".equals(mg.getTrangThai())) {
                messagingTemplate.convertAndSend("/topic/package-status/" + userId, 
                    "Bạn đã có gói đang hoạt động!");
                throw new RuntimeException("Bạn đã có gói đang hoạt động, không thể mua thêm");
            }
        }

        // 2. Nếu không vướng các điều kiện trên thì mới cho mua
        Goi goi = goiRepository.findById(goiId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gói cứu hộ"));

        MuaGoi muaGoi = new MuaGoi();
        muaGoi.setUserId(userId);
        muaGoi.setGoiId(goiId);
        muaGoi.setNgayMua(LocalDateTime.now());
        muaGoi.setTrangThai("PENDING");
        muaGoi.setNgayHetHan(muaGoi.getNgayMua().plusDays(goi.getThoiHan()));

        return muaGoiRepository.save(muaGoi);
    }

    public List<MuaGoiDto> getGoiByUserId(String userId) {
        List<MuaGoi> list = muaGoiRepository.findByUserId(userId);
        return list.stream().map(mg -> {
            String tenGoi = goiRepository.findById(mg.getGoiId()).map(Goi::getTen).orElse("Gói không xác định");
            return new MuaGoiDto(mg.getId(), mg.getUserId(), mg.getGoiId(), tenGoi, mg.getNgayMua(), mg.getNgayHetHan(), mg.getTrangThai());
        }).collect(Collectors.toList());
    }

    public void huyGoi(Long id, String userId) {
    MuaGoi mg = muaGoiRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy gói"));

    // ❗ Check chủ sở hữu
    if (!mg.getUserId().equals(userId)) {
        throw new RuntimeException("Bạn không có quyền hủy gói này");
    }

    // ❗ Check trạng thái
    if ("ACTIVE".equals(mg.getTrangThai())) {
        throw new RuntimeException("Gói đang hoạt động, không thể hủy!");
    }

    mg.setTrangThai("CANCELLED"); // Đổi trạng thái thay vì delete
    muaGoiRepository.save(mg);

    messagingTemplate.convertAndSend(
            "/topic/package-status/" + userId,
            "REFRESH"
    );
}

}