package com.example.suco.controller.admin;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.suco.dto.CameraMapDto;
import com.example.suco.model.BaoCaoSuCo;
import com.example.suco.model.Camera;
import com.example.suco.repository.BaoCaoSuCoRepository;
import com.example.suco.repository.CameraRepository;
import com.example.suco.service.CameraService;

@Controller
@RequestMapping("/admin/quan-ly-camera")
public class AdminCameraController {

    @Autowired
    private CameraService cameraService;

    @Autowired
    private CameraRepository cameraRepository;

    @Autowired
    private BaoCaoSuCoRepository baoCaoSuCoRepository;

    @GetMapping
    public String hienThiDanhSach(Model model) {
        model.addAttribute("danhSachCamera", cameraService.getAllCameras());
        model.addAttribute("listCameraChuaGan", cameraService.getCamerasChuaGan());
        model.addAttribute("activePage", "quan-ly-camera");
        return "admin/quan-ly-camera";
    }

    /**
     * FIX TOÀN BỘ ITC_11.1 ĐẾN ITC_11.6
     */
    @PostMapping(value = "/them", consumes = "multipart/form-data")
    @ResponseBody 
    public ResponseEntity<?> themCamera(
            @RequestParam(value = "tenCamera", required = false) String tenCamera,
            @RequestParam(value = "moTa", required = false) String moTa,
            @RequestParam(value = "kinhDo", required = false) String kinhDoStr,
            @RequestParam(value = "viDo", required = false) String viDoStr,
            @RequestParam(value = "anhCamera", required = false) MultipartFile anhCamera,
            @RequestParam(value = "videoFile", required = false) MultipartFile videoFile) {
        try {
            // 1. Kiểm tra trống tên camera (TC_09)
            if (tenCamera == null || tenCamera.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tên camera không được để trống!");
            }

            // 2. Kiểm tra độ dài tên camera (ITC_11.2)
            if (tenCamera.length() > 255) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tên camera không được vượt quá 255 ký tự!");
            }

            // 3. Kiểm tra độ dài mô tả (ITC_11.4)
            if (moTa != null && moTa.length() > 255) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mô tả không được vượt quá 255 ký tự!");
            }

            // 4. Kiểm tra trống Kinh độ/Vĩ độ (ITC_11.6)
            if (kinhDoStr == null || kinhDoStr.trim().isEmpty() || viDoStr == null || viDoStr.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Kinh độ và vĩ độ không được để trống!");
            }

            // 5. Kiểm tra độ dài Kinh độ/Vĩ độ (ITC_11.5)
            if (kinhDoStr.length() > 255 || viDoStr.length() > 255) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Độ dài tọa độ quá giới hạn cho phép!");
            }

            // 6. Kiểm tra định dạng số của tọa độ
            Double kinhDo;
            Double viDo;
            try {
                kinhDo = Double.parseDouble(kinhDoStr.trim());
                viDo = Double.parseDouble(viDoStr.trim());
            } catch (NumberFormatException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tọa độ phải là định dạng số hợp lệ!");
            }

            // Tạo đối tượng Camera nếu tất cả validate thành công
            Camera camera = new Camera();
            camera.setTenCamera(tenCamera.trim());
            camera.setMoTa(moTa != null && !moTa.trim().isEmpty() ? moTa.trim() : null);
            camera.setKinhDo(kinhDo);
            camera.setViDo(viDo);
            
            // Xử lý upload ảnh
            if (anhCamera != null && !anhCamera.isEmpty()) {
                String anhPath = cameraService.saveImage(anhCamera);
                camera.setAnhCamera(anhPath);
            }
            
            // Xử lý upload video
            if (videoFile != null && !videoFile.isEmpty()) {
                String videoPath = cameraService.saveVideo(videoFile);
                camera.setVideoUrl(videoPath);
            }
            
            cameraService.saveCamera(camera);
            return ResponseEntity.ok(camera);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping("/all-json")
    @ResponseBody
    public List<Camera> getAllCameraJson() {
        return cameraService.getAllCameras();
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<String> xoaCamera(@PathVariable Long id) {
        if (!cameraRepository.existsById(id)) {
            return ResponseEntity.status(404).body("Camera không tồn tại!");
        }
        cameraService.deleteCamera(id);
        return ResponseEntity.ok("Xóa camera thành công!");
    }

    @GetMapping("/{id}/detail")
    @ResponseBody
    public ResponseEntity<Camera> getCameraDetail(@PathVariable Long id) {
        return cameraRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/gan-toa-do/{id}")
    @ResponseBody
    public ResponseEntity<String> ganToaDoCamera(@PathVariable Long id,
                                                 @RequestParam("kinhDo") double kinhDo,
                                                 @RequestParam("viDo") double viDo) {
        return cameraRepository.findById(id).map(cam -> {
            cam.setKinhDo(kinhDo);
            cam.setViDo(viDo);
            cameraService.saveCamera(cam);
            return ResponseEntity.ok("Gán tọa độ thành công");
        }).orElse(ResponseEntity.status(404).body("Không tìm thấy camera"));
    }

    @GetMapping("/near-by-incident/{id}")
    @ResponseBody
    public List<CameraMapDto> getCameraByIncident(@PathVariable Long id) {
        BaoCaoSuCo report = baoCaoSuCoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự cố"));

        return cameraService.getCamerasNearIncident(
                report.getViDo(),
                report.getKinhDo()
        );
    }
}