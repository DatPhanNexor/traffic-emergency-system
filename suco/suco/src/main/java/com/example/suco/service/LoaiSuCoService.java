package com.example.suco.service;

import com.example.suco.model.LoaiSuCo;
import com.example.suco.repository.LoaiSuCoRepository;


import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.nio.file.Files;
import org.springframework.web.server.ResponseStatusException;
    import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public LoaiSuCo createLoaiSuCo(String ten, MultipartFile file) throws IOException {

        log.info("\nAdmin gửi tên loại sự cố: {}", ten);
    if (ten == null || ten.trim().isEmpty()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Tên loại sự cố không được để trống"
        );
    }
    

    LoaiSuCo l = new LoaiSuCo();
    l.setTen(ten);

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
    log.info("\nSau xử lý, tạo thành công loại sự cố:\nTen: {}\nIcon URL: {}", l.getTen(), l.getIconUrl());

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
public LoaiSuCo updateLoaiSuCo(Long id, String ten, MultipartFile file) throws IOException {
    LoaiSuCo entity = findById(id);

    if (ten != null && !ten.isEmpty()) {
        entity.setTen(ten);
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
