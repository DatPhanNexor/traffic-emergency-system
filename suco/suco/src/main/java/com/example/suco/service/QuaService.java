    package com.example.suco.service;

    import com.example.suco.dto.QuaDto;
    import com.example.suco.model.Qua;
    import com.example.suco.repository.QuaRepository;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;
    import java.io.IOException;
    import java.nio.file.*;
    import java.util.List;
    import java.util.UUID;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    @Service
    public class QuaService {

        private static final Logger log = LoggerFactory.getLogger(QuaService.class);


        @Autowired
        private QuaRepository quaRepository;

        private final String uploadDir = System.getProperty("user.dir") + "/uploads/";

        public List<Qua> getAllQua() {
            return quaRepository.findAll();
        }

        public void addQua(QuaDto quaDto) throws IOException {

    
        log.info("\n===== Admin tạo quà =====");
        log.info("\nTên: {}", quaDto.getTen());
        log.info("\nLoại: {}", quaDto.getLoai());
        log.info("\nĐiểm nhập vào: {}", quaDto.getDiem());
        log.info("\nMô tả: {}", quaDto.getMoTa());
        log.info("\nNgày kết thúc: {}", quaDto.getNgayKetThuc());

        if (quaDto.getDiem() == null) {
            log.error("ADD QUÀ FAILED - diem NULL | dto={}", quaDto);
            throw new IllegalArgumentException("Điểm đổi quà không được để trống");
        }
        if (quaDto.getDiem() <= 0) {
            log.error("ADD QUÀ FAILED - diem Không hợp lệ | dto={}", quaDto);
            throw new IllegalArgumentException("Điểm đổi quà phải lớn hơn 0");
        }

            String fileName = "default.png";
            if (quaDto.getHinhAnh() != null && !quaDto.getHinhAnh().isEmpty()) {
                fileName = UUID.randomUUID().toString() + "_" + quaDto.getHinhAnh().getOriginalFilename();
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                try (var inputStream = quaDto.getHinhAnh().getInputStream()) {
                    Files.copy(inputStream, uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                }
            }

            Qua qua = new Qua();
            qua.setTen(quaDto.getTen());
            qua.setLoai(quaDto.getLoai());
            qua.setMoTa(quaDto.getMoTa());
            qua.setDiem(quaDto.getDiem());
            qua.setHinhAnh(fileName);
            qua.setNgayKetThuc(quaDto.getNgayKetThuc());
            qua.setTrangThai(Qua.TrangThai.HOAT_DONG);
            // Trong file QuaService.java
    if (quaDto.getLoai() == Qua.LoaiQua.VOUCHER) {
        qua.setGiaTriGiamPercent(quaDto.getGiaTriGiamPercent());
        // Đảm bảo quaDto.getGiaTriToiDa() trả về BigDecimal
        qua.setGiaTriToiDa(quaDto.getGiaTriToiDa()); 
    } else {
            qua.setGiaTriGiamPercent(null);
            qua.setGiaTriToiDa(null);
        }
            quaRepository.save(qua);
    log.info("\nQuà đã được lưu với ID: {}", qua.getId());
    log.info("\nLoại quà: {}", qua.getLoai());
    log.info("\nĐiểm cần đổi: {}", qua.getDiem());
    log.info("\nTrạng thái: {}", qua.getTrangThai());
    log.info("\nNgày kết thúc: {}", qua.getNgayKetThuc());
        }

        public void deleteQua(Long id) {
        Qua qua = quaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy quà với id = " + id));

        quaRepository.delete(qua);
    }
    public void updateQua(Long id, QuaDto dto) {

            log.info("===== Bắt đầu cập nhật quà =====");
        log.info("ID: {}", id);
        log.info("DTO loai: {}", dto.getLoai());
        log.info("DTO trangThai: {}", dto.getTrangThai());
        log.info("DTO giamPercent: {}", dto.getGiaTriGiamPercent());
        log.info("DTO toiDa: {}", dto.getGiaTriToiDa());
        log.info("DTO ngayKetThuc: {}", dto.getNgayKetThuc());

        Qua qua = quaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quà"));

        if (dto.getTen() != null) {
            qua.setTen(dto.getTen());
        }

        if (dto.getMoTa() != null) {
            qua.setMoTa(dto.getMoTa());
        }

        if (dto.getLoai() != null) {
            qua.setLoai(dto.getLoai());
        }

        if (dto.getDiem() != null) {
            qua.setDiem(dto.getDiem());
        }

        if (dto.getNgayKetThuc() != null) {
    qua.setNgayKetThuc(dto.getNgayKetThuc());
}

        if (dto.getTrangThai() != null) {
            qua.setTrangThai(dto.getTrangThai());
        }

        if (dto.getHinhAnh() != null && !dto.getHinhAnh().isEmpty()) {
            String fileName = UUID.randomUUID().toString() + "_" + dto.getHinhAnh().getOriginalFilename();
            Path uploadPath = Paths.get(uploadDir);

            try {
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                try (var inputStream = dto.getHinhAnh().getInputStream()) {
                    Files.copy(inputStream, uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                }

                qua.setHinhAnh(fileName);
            } catch (IOException e) {
                throw new RuntimeException("Không thể lưu ảnh quà", e);
            }
        }

        // logic voucher
        if (dto.getLoai() != null) {
        qua.setLoai(dto.getLoai());

    if (dto.getLoai() == Qua.LoaiQua.VOUCHER) {
        if (dto.getGiaTriGiamPercent() != null) {
            qua.setGiaTriGiamPercent(dto.getGiaTriGiamPercent());
        }
        if (dto.getGiaTriToiDa() != null) {
            qua.setGiaTriToiDa(dto.getGiaTriToiDa());
        }
    }
}
        
        

        quaRepository.save(qua);
        log.info("===== Giá trị sau khi cập nhật =====");
        log.info("NEW giamPercent: {}", qua.getGiaTriGiamPercent());
        log.info("NEW toiDa: {}", qua.getGiaTriToiDa());
        log.info("NEW trangThai: {}", qua.getTrangThai());
    }
    public void updateStatus(Long id, Qua.TrangThai trangThai) {
        Qua qua = quaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy"));

        qua.setTrangThai(trangThai);

        quaRepository.save(qua);
    }
    }
