package com.example.suco.service;

import com.example.suco.dto.CameraMapDto;
import com.example.suco.model.Camera;
import com.example.suco.repository.CameraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import ch.hsr.geohash.GeoHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CameraService {
private static final Logger log = LoggerFactory.getLogger(CameraService.class);
    @Autowired
    private CameraRepository cameraRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 1. Lấy tất cả camera (Dạng Model cho Admin quản lý bảng)
    public List<Camera> getAllCameras() {
        return cameraRepository.findAll();
    }

    // 2. Lấy danh sách camera chưa gán tọa độ (Dùng cho Select-box trên FE)
    public List<Camera> getCamerasChuaGan() {
        return cameraRepository.findAll().stream()
                .filter(c -> c.getKinhDo() == null || c.getKinhDo() == 0.0)
                .collect(Collectors.toList());
    }

    // 3. Lấy tất cả camera dưới dạng DTO (Dùng cho Mapbox API)
    public List<CameraMapDto> getAllCameraForMap() {
    return cameraRepository.findAll().stream()
            .filter(c -> c.getKinhDo() != null && c.getKinhDo() != 0.0)
            .map(c -> new CameraMapDto(
                c.getId(), 
                c.getTenCamera(), 
                c.getKinhDo(), 
                c.getViDo(), 
                c.getAnhCamera(), 
                c.getVideoUrl(), 
                c.getMoTa(),
                 0.0
))
                
            .collect(Collectors.toList());
}

    // 4. Lưu hoặc Cập nhật vị trí Camera
    @Transactional
    public Camera saveCamera(Camera camera) {
        // TÍNH TOÁN GEOHASH 8 (Đảm bảo luôn tính trước khi lưu)
        if (camera.getViDo() != null && camera.getKinhDo() != null && camera.getViDo() != 0) {
            String gh = GeoHash.withCharacterPrecision(camera.getViDo(), camera.getKinhDo(), 8).toBase32();
            camera.setGeohash(gh);
        }

        Camera saved;
        if (camera.getId() != null) {
            saved = cameraRepository.findById(camera.getId())
                    .map(existing -> {
                        existing.setKinhDo(camera.getKinhDo());
                        existing.setViDo(camera.getViDo());
                        existing.setGeohash(camera.getGeohash()); // CẬP NHẬT CẢ GEOHASH VÀO DB
                        if (camera.getTenCamera() != null && !camera.getTenCamera().isEmpty()) {
                            existing.setTenCamera(camera.getTenCamera());
                        }
                        return cameraRepository.save(existing);
                    }).orElseGet(() -> cameraRepository.save(camera));
        } else {
            if (camera.getKinhDo() == null) camera.setKinhDo(0.0);
            if (camera.getViDo() == null) camera.setViDo(0.0);
            saved = cameraRepository.save(camera);
        }

        CameraMapDto dto = new CameraMapDto(saved.getId(), saved.getTenCamera(), saved.getKinhDo(), saved.getViDo(), saved.getAnhCamera(), saved.getVideoUrl(), saved.getMoTa(), 0.0);
        messagingTemplate.convertAndSend("/topic/camera", dto);
        return saved;
    }

    /**
     * TÌM CAMERA GẦN SỰ CỐ TRONG BÁN KÍNH 20M
     * Không lưu vào DB, chỉ tính toán động để hiển thị cho Admin
     */
    public List<CameraMapDto> getCamerasNearIncident(double lat, double lng) {
        log.info("========== KIỂM TRA CAMERA GẦN SỰ CỐ ==========");
        log.info("[Vị trí sự cố]: {}, {}", lat, lng);

        // 1. Lấy Geohash cấp 8 của sự cố
        GeoHash center = GeoHash.withCharacterPrecision(lat, lng, 8);
        List<String> area = new ArrayList<>();
        area.add(center.toBase32());
        for (GeoHash adj : center.getAdjacent()) {
            area.add(adj.toBase32());
        }
        log.info("[Geohash]: Quét vùng 9 ô xung quanh mã: {}", center.toBase32());

        // 2. Query DB theo index Geohash
        List<Camera> candidates = cameraRepository.findByGeohashIn(area);
        log.info("[DB]: Tìm thấy {} camera tiềm năng trong các ô Geohash", candidates.size());

        // 3. Lọc Haversine chính xác 20m
        List<CameraMapDto> result = candidates.stream()
        .map(c -> {
    double distance = tinhKhoangCach(lat, lng, c.getViDo(), c.getKinhDo());

    log.info("Camera {} distance = {} km", c.getId(), distance);

    if (distance > 0.02) return null;

    CameraMapDto dto = new CameraMapDto(
            c.getId(),
            c.getTenCamera(),
            c.getKinhDo(),
            c.getViDo(),
            c.getAnhCamera(),
            c.getVideoUrl(),
            c.getMoTa(),
            Math.round(distance * 1000 * 100.0) / 100.0

    );
    return dto;
})
        .filter(dto -> dto != null)
        .collect(Collectors.toList());

        log.info("[Kết quả]: Tổng cộng {} camera khả dụng.", result.size());
        log.info("===============================================");
        
        return result;
    }

    private double tinhKhoangCach(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Bán kính Trái Đất (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // 5. Xóa Camera
    @Transactional
    public void deleteCamera(Long id) {
        cameraRepository.deleteById(id);
        // 🔥 Bắn Socket thông báo xóa cho Android ẩn Marker
        messagingTemplate.convertAndSend("/topic/camera-delete", id);
    }

    // 6. Lưu ảnh camera
    public String saveImage(MultipartFile imageFile) {
        try {
            String filename = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
            Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "cameras");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            Files.copy(imageFile.getInputStream(), uploadDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/cameras/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Lỗi lưu ảnh camera: " + e.getMessage());
        }
    }

    // 7. Lưu video demo camera
    // Trong CameraService.java
public String saveVideo(MultipartFile file) throws IOException {
    // 1. Định nghĩa thư mục lưu trữ vật lý
    String uploadDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "cameras" + File.separator + "videos";
    File dir = new File(uploadDir);
    if (!dir.exists()) dir.mkdirs();

    // 2. Tạo tên file duy nhất
    String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
    Path filePath = Paths.get(uploadDir, fileName);

    // 3. Lưu file
    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

    // 4. Trả về đường dẫn để lưu vào Database (Khớp với WebMvcConfig)
    return "/uploads/cameras/videos/" + fileName;
}

    
}