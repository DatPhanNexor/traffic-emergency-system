package com.example.suco.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.suco.dto.TruSoMapDto;
import com.example.suco.model.TruSo;
import com.example.suco.repository.TruSoRepository;

import ch.hsr.geohash.GeoHash;

@Service
public class TruSoService {
    private static final Logger log = LoggerFactory.getLogger(TruSoService.class);

    @Autowired
    private TruSoRepository truSoRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public TruSo saveTruSo(TruSo truSo) {
        
        // ================= [FIX B05] VALIDATE TỌA ĐỘ =================
        // Kiểm tra null (Chỉ hoạt động nếu TruSo.java dùng kiểu Double thay vì double)
        if (truSo.getKinhDo() == null || truSo.getViDo() == null) {
            throw new RuntimeException("Kinh độ và vĩ độ không được để trống");
        }

        // Kiểm tra phạm vi tọa độ hợp lệ
        if (truSo.getViDo() < -90 || truSo.getViDo() > 90 || 
            truSo.getKinhDo() < -180 || truSo.getKinhDo() > 180) {
            throw new RuntimeException("Tọa độ không hợp lệ (Vĩ độ: -90 đến 90, Kinh độ: -180 đến 180)");
        }

        // Sau khi validate tọa độ xong mới tính Geohash để tránh lỗi hệ thống
        String gh = GeoHash.withCharacterPrecision(truSo.getViDo(), truSo.getKinhDo(), 6).toBase32();
        truSo.setGeohash(gh);

        // ================= VALIDATE TÊN TRỤ SỞ =================
        if (truSo.getTenTruSo() == null || truSo.getTenTruSo().trim().isEmpty()) {
            throw new RuntimeException("Tên trụ sở không được để trống");
        }
        if (truSo.getTenTruSo().length() > 255) {
            throw new RuntimeException("Tên trụ sở không được vượt quá 255 ký tự");
        }

        // ================= VALIDATE USERNAME =================
        String username = truSo.getTenDangNhap();
        if (username == null || username.length() < 5 || username.length() > 20 || username.contains(" ")) {
            throw new RuntimeException("Tên đăng nhập phải 5-20 ký tự, không chứa khoảng trắng");
        }

        // CHECK TRÙNG USERNAME
        boolean isDuplicate = truSoRepository.existsByTenDangNhap(username);
        if (truSo.getId() == null) {
            if (isDuplicate) throw new RuntimeException("Tên đăng nhập đã tồn tại");
        } else {
            TruSo existing = truSoRepository.findById(truSo.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trụ sở"));
            if (!existing.getTenDangNhap().equals(username) && isDuplicate) {
                throw new RuntimeException("Tên đăng nhập đã tồn tại");
            }
        }

        // ================= [FIX B04] VALIDATE PASSWORD CHI TIẾT =================
        String password = truSo.getMatKhau();
        if (truSo.getId() == null || (password != null && !password.isBlank())) {
            if (password == null || password.trim().isEmpty()) {
                throw new RuntimeException("Mật khẩu không được để trống");
            }
            if (password.length() > 256) {
                throw new RuntimeException("Mật khẩu không được vượt quá 256 ký tự");
            }
            if (password.length() < 8) {
                throw new RuntimeException("Mật khẩu phải có ít nhất 8 ký tự");
            }
            if (!password.matches(".*[A-Z].*")) {
                throw new RuntimeException("Mật khẩu phải có ít nhất 1 chữ hoa");
            }
            if (!password.matches(".*[a-z].*")) {
                throw new RuntimeException("Mật khẩu phải có ít nhất 1 chữ thường");
            }
            if (!password.matches(".*\\d.*")) {
                throw new RuntimeException("Mật khẩu phải có ít nhất 1 số");
            }
            if (!password.matches(".*[@$!%*?&].*")) {
                throw new RuntimeException("Mật khẩu phải có ít nhất 1 ký tự đặc biệt");
            }
        }

        // ================= LƯU DỮ LIỆU =================
        TruSo saved;
        if (truSo.getId() != null) {
            saved = truSoRepository.findById(truSo.getId())
                    .map(existing -> {
                        existing.setKinhDo(truSo.getKinhDo());
                        existing.setViDo(truSo.getViDo());
                        existing.setGeohash(gh);
                        if (truSo.getTenTruSo() != null) existing.setTenTruSo(truSo.getTenTruSo().trim());
                        
                        if (truSo.getMatKhau() != null && !truSo.getMatKhau().isBlank() 
                            && !truSo.getMatKhau().equals(existing.getMatKhau())) {
                            existing.setMatKhau(passwordEncoder.encode(truSo.getMatKhau()));
                        }
                        return truSoRepository.save(existing);
                    })
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy trụ sở ID: " + truSo.getId()));
        } else {
            if (truSo.getMatKhau() != null && !truSo.getMatKhau().isBlank()) {
                truSo.setMatKhau(passwordEncoder.encode(truSo.getMatKhau()));
            }
            saved = truSoRepository.save(truSo);
        }

        messagingTemplate.convertAndSend("/topic/tru-so", new TruSoMapDto(saved.getId(), saved.getTenTruSo(), saved.getKinhDo(), saved.getViDo()));
        return saved;
    }

    public List<TruSoMapDto> getAllTruSoForMap() {
        return truSoRepository.findAll().stream()
                .map(ts -> new TruSoMapDto(ts.getId(), ts.getTenTruSo(), ts.getKinhDo(), ts.getViDo()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteTruSo(Long id) {
        TruSo ts = truSoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trụ sở không tồn tại"));
        truSoRepository.delete(ts);
        messagingTemplate.convertAndSend("/topic/tru-so-delete", id);
    }

    public TruSo timTruSoGanNhat(double userLat, double userLng) {
        log.info("--- ĐANG TÌM TRỤ SỞ GẦN NHẤT ---");
        List<TruSo> candidates = new ArrayList<>();
        int precision = 6; 

        while (precision >= 4 && candidates.isEmpty()) {
            GeoHash center = GeoHash.withCharacterPrecision(userLat, userLng, precision);
            String currentHash = center.toBase32();
            List<String> searchPrefixes = new ArrayList<>();
            searchPrefixes.add(currentHash);
            for (GeoHash adjacent : center.getAdjacent()) {
                searchPrefixes.add(adjacent.toBase32());
            }

            if (precision == 6) {
                candidates = truSoRepository.findByGeohashIn(searchPrefixes);
            } else {
                for (String prefix : searchPrefixes) {
                    candidates.addAll(truSoRepository.findByGeohashStartingWith(prefix));
                }
                candidates = new ArrayList<>(new HashSet<>(candidates));
            }
            if (candidates.isEmpty()) precision--; 
        }

        if (candidates.isEmpty()) candidates = truSoRepository.findAll();

        TruSo ganNhat = null;
        double minD = Double.MAX_VALUE;

        for (TruSo ts : candidates) {
            double d = tinhKhoangCach(userLat, userLng, ts.getViDo(), ts.getKinhDo());
            if (d < minD) {
                minD = d;
                ganNhat = ts;
            }
        }
        return ganNhat;
    }

    private double tinhKhoangCach(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public TruSo timTruSoTheoId(Long idTruSo) {
        return truSoRepository.findById(idTruSo).orElse(null);
    }

    public List<TruSo> layTatCaTruSo() {
        return truSoRepository.findAll();
    }
}