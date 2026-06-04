package com.example.suco.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.suco.model.LoaiSuCo;
import com.example.suco.repository.LoaiSuCoRepository;

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
     * FIX CHO ITC_14.3 & ITC_14.4: Chặn trùng tên và tên trống
     */
    public LoaiSuCo createLoaiSuCo(String ten, MultipartFile file) throws IOException {
        log.info("Đang tạo loại sự cố với tên: {}", ten);

        // 1. Kiểm tra trống (ITC_14.4)
        if (ten == null || ten.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên loại sự cố không được để trống");
        }

        // 2. KIỂM TRA TRÙNG TÊN (Fix ITC_14.3 - Bug B04)
        // Lưu ý: Dùng .trim() để tránh khoảng trắng thừa gây sai lệch
        if (repository.findByTen(ten.trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên loại sự cố đã tồn tại");
        }

        LoaiSuCo l = new LoaiSuCo();
        l.setTen(ten.trim());

        if (file != null && !file.isEmpty()) {
            String filename = file.getOriginalFilename();
            String uploadDir = System.getProperty("user.dir") + "/uploads/icons/";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Files.copy(file.getInputStream(),
                    uploadPath.resolve(filename),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            l.setIconUrl("/uploads/icons/" + filename);
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
     * FIX CHO ITC_17.3: Chặn trùng tên khi cập nhật
     */
    public LoaiSuCo updateLoaiSuCo(Long id, String ten, MultipartFile file) throws IOException {
        LoaiSuCo entity = findById(id);

        if (ten != null && !ten.trim().isEmpty()) {
            String trimmedTen = ten.trim();
            
            // Nếu tên mới khác tên cũ, phải check xem có trùng với record khác không
            repository.findByTen(trimmedTen).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên loại sự cố đã tồn tại");
                }
            });
            
            entity.setTen(trimmedTen);
        }

        if (file != null && !file.isEmpty()) {
            String filename = file.getOriginalFilename();
            String uploadDir = System.getProperty("user.dir") + "/uploads/icons/";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Files.copy(file.getInputStream(),
                    uploadPath.resolve(filename),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            entity.setIconUrl("/uploads/icons/" + filename);
        }

        return repository.save(entity);
    }
}