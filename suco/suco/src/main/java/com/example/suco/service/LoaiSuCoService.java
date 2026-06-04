package com.example.suco.service;

import com.example.suco.model.LoaiSuCo;
import com.example.suco.repository.LoaiSuCoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class LoaiSuCoService {

    private static final Logger log = LoggerFactory.getLogger(LoaiSuCoService.class);
    private final LoaiSuCoRepository repository;

    public LoaiSuCoService(LoaiSuCoRepository repository) {
        this.repository = repository;
    }

    public List<LoaiSuCo> getLoaiSuCo() {
        return repository.findAll();
    }

    /**
     * FIX CHO ITC_14.3 & ITC_14.4: Chặn trùng tên và tên trống khi tạo mới
     */
    public LoaiSuCo createLoaiSuCo(String ten, MultipartFile file) throws IOException {
        log.info("Đang tạo loại sự cố với tên: {}", ten);

        // 1. Kiểm tra trống (ITC_14.4)
        if (ten == null || ten.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên loại sự cố không được để trống");
        }

        String trimmedTen = ten.trim();

        // 2. KIỂM TRA TRÙNG TÊN (Dùng List để tránh lỗi NonUniqueResultException nếu DB đã lỡ trùng)
        List<LoaiSuCo> existingList = repository.findAllByTen(trimmedTen);
        if (!existingList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên loại sự cố đã tồn tại");
        }

        LoaiSuCo l = new LoaiSuCo();
        l.setTen(trimmedTen);

        // Xử lý upload file icon
        if (file != null && !file.isEmpty()) {
            l.setIconUrl(saveIcon(file));
        }

        return repository.save(l);
    }

    public void deleteLoaiSuCo(Long id) {
        LoaiSuCo entity = findById(id);
        repository.delete(entity);
    }

    public LoaiSuCo findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Loại sự cố không tồn tại"
            ));
    }

    /**
     * FIX CHO ITC_17.3: Chặn trùng tên khi cập nhật (B04)
     */
    public LoaiSuCo updateLoaiSuCo(Long id, String ten, MultipartFile file) throws IOException {
        LoaiSuCo entity = findById(id);

        // 1. Kiểm tra logic thay đổi tên
        if (ten != null && !ten.trim().isEmpty()) {
            String trimmedTen = ten.trim();
            
            // Tìm tất cả bản ghi có tên này
            List<LoaiSuCo> others = repository.findAllByTen(trimmedTen);
            for (LoaiSuCo other : others) {
                // Nếu tìm thấy một bản ghi trùng tên mà KHÔNG PHẢI là bản ghi đang sửa -> Báo lỗi
                if (!other.getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên loại sự cố đã tồn tại");
                }
            }
            entity.setTen(trimmedTen);
        } else if (ten != null && ten.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên không được để trống");
        }

        // 2. Xử lý cập nhật file icon
        if (file != null && !file.isEmpty()) {
            entity.setIconUrl(saveIcon(file));
        }

        return repository.save(entity);
    }

    /**
     * Hàm phụ trợ lưu file icon
     */
    private String saveIcon(MultipartFile file) throws IOException {
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String uploadDir = System.getProperty("user.dir") + "/uploads/icons/";
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Files.copy(file.getInputStream(),
                uploadPath.resolve(filename),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/icons/" + filename;
    }
}