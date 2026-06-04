package com.example.suco.controller.admin;

import java.util.List;

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

            if (tenCamera == null || tenCamera.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tên camera không được để trống!");
            }


            if (tenCamera.length() > 255) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tên camera không được vượt quá 255 ký tự!");
            }

            Camera camera = new Camera();
            camera.setTenCamera(tenCamera.trim());
            camera.setMoTa(moTa != null && !moTa.trim().isEmpty() ? moTa.trim() : null);
            

            Double kinhDo = 0.0;
            Double viDo = 0.0;
            try {
                if (kinhDoStr != null && !kinhDoStr.trim().isEmpty()) {
                    kinhDo = Double.parseDouble(kinhDoStr);
                }
            } catch (NumberFormatException e) {

            }
            try {
                if (viDoStr != null && !viDoStr.trim().isEmpty()) {
                    viDo = Double.parseDouble(viDoStr);
                }
            } catch (NumberFormatException e) {

            }
            
            camera.setKinhDo(kinhDo);
            camera.setViDo(viDo);
            

            if (anhCamera != null && !anhCamera.isEmpty()) {
                String anhPath = cameraService.saveImage(anhCamera);
                camera.setAnhCamera(anhPath);
            }
            

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