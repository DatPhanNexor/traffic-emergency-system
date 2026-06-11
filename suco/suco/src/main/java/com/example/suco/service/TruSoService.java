package com.example.suco.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        // 1. Validate tọa độ và sinh Geohash (Fix B05)
        validateCoordinates(truSo);
        String gh = GeoHash.withCharacterPrecision(truSo.getViDo(), truSo.getKinhDo(), 6).toBase32();
        truSo.setGeohash(gh);

        // 2. Validate thông tin cơ bản và Username (Fix Sonar Generic Exception)
        validateBasicInfo(truSo);
        validateUsernameUniqueness(truSo);

        // 3. Validate mật khẩu chi tiết (Fix B04)
        validatePasswordComplexity(truSo);

        // 4. Lưu dữ liệu (Đã tách hàm để giảm Complexity xuống dưới 15)
        TruSo saved = finalizeAndSave(truSo, gh);

        // 5. Thông báo qua WebSocket
        messagingTemplate.convertAndSend("/topic/tru-so", 
            new TruSoMapDto(saved.getId(), saved.getTenTruSo(), saved.getKinhDo(), saved.getViDo()));
        
        return saved;
    }

    private TruSo finalizeAndSave(TruSo truSo, String gh) {
        if (truSo.getId() != null) {
            return processUpdate(truSo, gh);
        } 
        
        if (truSo.getMatKhau() != null && !truSo.getMatKhau().isBlank()) {
            truSo.setMatKhau(passwordEncoder.encode(truSo.getMatKhau()));
        }
        return truSoRepository.save(truSo);
    }

    // --- CÁC HÀM PHỤ TRỢ (HELPER METHODS) ĐÃ ĐƯỢC TỐI ƯU ---

    private void validateCoordinates(TruSo truSo) {
        if (truSo.getKinhDo() == null || truSo.getViDo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kinh độ và vĩ độ không được để trống");
        }
        if (truSo.getViDo() < -90 || truSo.getViDo() > 90 || truSo.getKinhDo() < -180 || truSo.getKinhDo() > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tọa độ không hợp lệ");
        }
    }

    private void validateBasicInfo(TruSo truSo) {
        if (truSo.getTenTruSo() == null || truSo.getTenTruSo().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên trụ sở không được để trống");
        }
        if (truSo.getTenTruSo().length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên trụ sở không được vượt quá 255 ký tự");
        }
    }

    private void validateUsernameUniqueness(TruSo truSo) {
        String username = truSo.getTenDangNhap();
        if (username == null || username.length() < 5 || username.length() > 20 || username.contains(" ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên đăng nhập phải 5-20 ký tự, không chứa khoảng trắng");
        }

        boolean isDuplicate = truSoRepository.existsByTenDangNhap(username);
        if (truSo.getId() == null) {
            if (isDuplicate) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên đăng nhập đã tồn tại");
        } else {
            TruSo existing = truSoRepository.findById(truSo.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy trụ sở"));
            if (!existing.getTenDangNhap().equals(username) && isDuplicate) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên đăng nhập đã tồn tại");
            }
        }
    }

    private void validatePasswordComplexity(TruSo truSo) {
        String password = truSo.getMatKhau();
        if (truSo.getId() == null || (password != null && !password.isBlank())) {
            checkPasswordRules(password);
        }
    }

    private void checkPasswordRules(String password) {
        if (password.trim().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu không được để trống");
        if (password.length() > 256) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu không được vượt quá 256 ký tự");
        if (password.length() < 8) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu phải có ít nhất 8 ký tự");
        if (!password.matches(".*[A-Z].*")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu phải có ít nhất 1 chữ hoa");
        if (!password.matches(".*[a-z].*")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu phải có ít nhất 1 chữ thường");
        if (!password.matches(".*\\d.*")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu phải có ít nhất 1 số");
        if (!password.matches(".*[@$!%*?&].*")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu phải có ít nhất 1 ký tự đặc biệt");
    }

    private TruSo processUpdate(TruSo truSo, String gh) {
        return truSoRepository.findById(truSo.getId())
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy trụ sở ID: " + truSo.getId()));
    }

    public List<TruSoMapDto> getAllTruSoForMap() {
        return truSoRepository.findAll().stream()
                .map(ts -> new TruSoMapDto(ts.getId(), ts.getTenTruSo(), ts.getKinhDo(), ts.getViDo()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteTruSo(Long id) {
        TruSo ts = truSoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trụ sở không tồn tại"));
        truSoRepository.delete(ts);
        messagingTemplate.convertAndSend("/topic/tru-so-delete", id);
    }

    public TruSo timTruSoGanNhat(double userLat, double userLng) {
        log.info("--- Tìm kiếm định vị trụ sở gần nhất ---");
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