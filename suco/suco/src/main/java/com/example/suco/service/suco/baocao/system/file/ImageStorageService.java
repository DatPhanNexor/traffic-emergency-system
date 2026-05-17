package com.example.suco.service.suco.baocao.system.file;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class ImageStorageService {

    public String saveBase64Image(String base64Data) {

        try {

            String fileName =
                    System.currentTimeMillis() + "_report.jpg";

            Path uploadPath = Paths.get(
                    System.getProperty("user.dir"),
                    "uploads",
                    "reports"
            );

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String base64Image =
                    base64Data.contains(",")
                            ? base64Data.split(",")[1]
                            : base64Data;

            Files.write(
                    uploadPath.resolve(fileName),
                    java.util.Base64.getDecoder().decode(base64Image)
            );

            return "/uploads/reports/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String saveMultipartImage(MultipartFile image) {

        try {

            String filename =
                    System.currentTimeMillis()
                            + "_"
                            + image.getOriginalFilename();

            Path uploadDir = Paths.get(
                    System.getProperty("user.dir"),
                    "uploads",
                    "reports"
            );

            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Files.copy(
                    image.getInputStream(),
                    uploadDir.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING
            );

            return "/uploads/reports/" + filename;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}