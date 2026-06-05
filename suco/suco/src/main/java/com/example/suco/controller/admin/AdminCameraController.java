package com.example.suco.controller.admin;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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

    private static final Logger log = LoggerFactory.getLogger(AdminCameraController.class);

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
     * FIX SONAR: Tách hàm để giảm độ phức tạp (Cognitive Complexity)
     * Thay ResponseEntity<?> bằng ResponseEntity<Object>
     */
    @PostMapping(value = "/them", consumes = "multipart/form-data")
    @ResponseBody 
    public ResponseEntity<Object> themCamera(
            @RequestParam(value = "tenCamera", required = false) String tenCamera,
            @RequestParam(value = "moTa", required = false) String moTa,
            @RequestParam(value = "kinhDo", required = false) String kinhDoStr,
            @RequestParam(value = "viDo", required = false) String viDoStr,
            @RequestParam(value = "anhCamera", required = false) MultipartFile anhCamera,
            @RequestParam(value = "videoFile", required = false) MultipartFile videoFile) {
        
        // 1. Validate đầu vào (Đã tách hàm)
        String errorMessage = validateCameraInput(tenCamera, moTa, kinhDoStr, viDoStr);
        if (errorMessage != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
        }

        try {
            Camera camera = new Camera();
            camera.setTenCamera(tenCamera.trim());
            camera.setMoTa(moTa != null && !moTa.trim().isEmpty() ? moTa.trim() : null);
            
            // 2. Parse tọa độ an toàn (Đã tách hàm)
            camera.setKinhDo(Double.parseDouble(kinhDoStr.trim()));
            camera.setViDo(Double.parseDouble(viDoStr.trim()));
            
            // 3. Xử lý upload files (Đã tách hàm)
            handleCameraUploads(camera, anhCamera, videoFile);
            
            cameraService.saveCamera(camera);
            return ResponseEntity.ok(camera);

        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tọa độ phải là định dạng số hợp lệ!");
        } catch (Exception e) {
            log.error("Lỗi hệ thống khi thêm camera: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống.");
        }
    }

    // Hàm phụ trợ 1: Kiểm tra tính hợp lệ của dữ liệu (Giảm Complexity)
    private String validateCameraInput(String ten, String moTa, String kinhDo, String viDo) {
        if (ten == null || ten.trim().isEmpty()) return "Tên camera không được để trống!";
        if (ten.length() > 255) return "Tên camera không được vượt quá 255 ký tự!";
        if (moTa != null && moTa.length() > 255) return "Mô tả không được vượt quá 255 ký tự!";
        if (kinhDo == null || kinhDo.trim().isEmpty() || viDo == null || viDo.trim().isEmpty()) {
            return "Kinh độ và vĩ độ không được để trống!";
        }
        if (kinhDo.length() > 255 || viDo.length() > 255) return "Độ dài tọa độ quá giới hạn!";
        return null;
    }

    // Hàm phụ trợ 2: Xử lý upload ảnh và video (Giảm Complexity)
    private void handleCameraUploads(Camera camera, MultipartFile anh, MultipartFile video) throws java.io.IOException {
        if (anh != null && !anh.isEmpty()) {
            camera.setAnhCamera(cameraService.saveImage(anh));
        }
        if (video != null && !video.isEmpty()) {
            camera.setVideoUrl(cameraService.saveVideo(video));
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Camera không tồn tại!");
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
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy camera"));
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