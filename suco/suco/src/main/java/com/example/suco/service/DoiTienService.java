package com.example.suco.service;

import com.example.suco.dto.DoiTienDto;
import com.example.suco.model.DoiTien;
import com.example.suco.model.User;
import com.example.suco.repository.DoiTienRepository;
import com.example.suco.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoiTienService {
    @Autowired private DoiTienRepository doiTienRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    private final long HE_SO = 100L; // 10 điểm * 100 = 1000 VNĐ

    @Transactional
public boolean thucHienDoiTien(String uid, DoiTienDto dto) {
    if (dto.getSoDiemDoi() <= 0) return false;

    // ✅ dùng uid từ token
    User user = userRepository.findById(uid).orElse(null);

    if (user == null || user.getTotalPoints() < dto.getSoDiemDoi()) return false;

    // 1. Trừ điểm
    user.setTotalPoints(user.getTotalPoints() - dto.getSoDiemDoi());
    userRepository.save(user);

    long giaTriGiaoDich = dto.getSoDiemDoi() * HE_SO;

    if ("QUYEN_GOP".equals(dto.getLoaiDoi())) {
        LocalDateTime start = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime end = LocalDateTime.now().with(LocalTime.MAX);

        DoiTien existing = doiTienRepository.findFirstByUserIdAndLoaiDoiAndNgayDoiBetween(
                uid, "QUYEN_GOP", start, end
        );

        if (existing != null) {
            existing.setSoDiemDoi(existing.getSoDiemDoi() + dto.getSoDiemDoi());

            long giaTriCu = (existing.getGiaTri() != null) ? existing.getGiaTri() : 0L;
            existing.setGiaTri(giaTriCu + giaTriGiaoDich);

            existing.setNgayDoi(LocalDateTime.now());
            doiTienRepository.save(existing);
        } else {
            saveNewRecord(uid, dto, giaTriGiaoDich);
        }

        broadcastFundStats();
    } else {
        saveNewRecord(uid, dto, giaTriGiaoDich);
    }

    messagingTemplate.convertAndSend("/topic/user-stats/" + uid, user);

    return true;
}

    private void saveNewRecord(String uid, DoiTienDto dto, long giaTri) {
        DoiTien log = new DoiTien(uid, dto.getSoDiemDoi(), giaTri, dto.getLoaiDoi(), LocalDateTime.now());
        doiTienRepository.save(log);
    }

public void broadcastFundStats() {
    Long tongGiaTri = doiTienRepository.sumAllDonationValues();
    
    Map<String, Object> payload = new HashMap<>();
    payload.put("tongGiaTri", tongGiaTri != null ? tongGiaTri : 0L);
    payload.put("lichSuVinhDanh", getFormattedVinhDanh()); // Dùng chung hàm chuẩn hóa

    messagingTemplate.convertAndSend("/topic/public-fund", payload);
}
// Thêm hàm này vào DoiTienService.java
public List<Map<String, Object>> getFormattedVinhDanh() {
    List<com.example.suco.model.DoiTien> logs = doiTienRepository.findTop10ByLoaiDoiOrderByNgayDoiDesc("QUYEN_GOP");
    
    return logs.stream().map(log -> {
        User u = userRepository.findById(log.getUserId()).orElse(null);
        Map<String, Object> map = new HashMap<>();
        // Quan trọng: key "userId" nhưng giá trị là Tên (để khớp Android)
        map.put("userId", u != null ? u.getName() : "Ẩn danh"); 
        map.put("email", u != null ? u.getEmail() : "");
        map.put("soDiemDoi", log.getSoDiemDoi());
        map.put("giaTri", log.getGiaTri() != null ? log.getGiaTri() : 0L);
        map.put("ngayDoi", log.getNgayDoi() != null ? log.getNgayDoi().toString() : "");
        return map;
    }).collect(Collectors.toList());
}
public List<DoiTien> getLichSu(String uid, String loai) {
    if (loai != null && !loai.isEmpty()) {
        return doiTienRepository.findByUserIdAndLoaiDoi(uid, loai);
    }
    return doiTienRepository.findByUserId(uid);
}
public List<DoiTien> getAllLichSu(String loai) {
    if (loai != null && !loai.isEmpty()) {
        // Chuẩn hóa sang Uppercase để khớp với Enum hoặc dữ liệu trong DB
        return doiTienRepository.findByLoaiDoi(loai.toUpperCase());
    }
    return doiTienRepository.findAll();
}
}