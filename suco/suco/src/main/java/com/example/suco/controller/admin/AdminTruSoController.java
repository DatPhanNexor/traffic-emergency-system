package com.example.suco.controller.admin;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.suco.dto.TruSoMapDto;
import com.example.suco.model.TruSo;
import com.example.suco.repository.TruSoRepository;
import com.example.suco.service.TruSoService;

@Controller
@RequestMapping("/admin/quan-ly-tru-so")
public class AdminTruSoController {

    // FIX SONAR: Sử dụng Logger thay cho System.out
    private static final Logger log = LoggerFactory.getLogger(AdminTruSoController.class);

    @Autowired
    private TruSoService truSoService;
    
    @Autowired
    private TruSoRepository truSoRepository;

    @GetMapping
    public String hienThiDanhSach(Model model) {
        model.addAttribute("danhSachTruSo", truSoRepository.findAll());
        model.addAttribute("activePage", "quan-ly-tru-so");
        return "admin/quan-ly-tru-so";
    }

    @GetMapping("/all")
    @ResponseBody
    public List<TruSoMapDto> getAllTruSo() {
        return truSoService.getAllTruSoForMap();
    }

    /**
     * METHOD 1: Xử lý MULTIPART_FORM_DATA (Dữ liệu từ giao diện Web)
     * Đã giữ nguyên các dòng Log "Lửa" của bạn nhưng chuyển sang chuẩn Logger
     */
@PostMapping(value = "/them", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<Object> themTruSoMultipart(@ModelAttribute TruSoRequest request) {
        log.info("🔥 ===== VÀO CONTROLLER /them TRU SO (MULTIPART) =====");
        // FIX SONAR: Chỉ log thông báo hành động, không log dữ liệu thô từ user
        log.info("📌 Đang xử lý dữ liệu form-data từ trình duyệt...");
        
        com.example.suco.model.TruSo truSo = mapToEntity(request);
        truSoService.saveTruSo(truSo);
        
        log.info("✅ ĐÃ LƯU TRỤ SỞ THÀNH CÔNG");
        return buildSuccessResponse(truSo, "Thêm trụ sở thành công!");
    }

    @PostMapping(value = "/them", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> themTruSoJson(@RequestBody TruSoRequest request) {
        log.info("🔥 ===== VÀO CONTROLLER /them TRU SO (JSON) =====");
        log.info("📌 Đang xử lý dữ liệu JSON từ API...");

        com.example.suco.model.TruSo truSo = mapToEntity(request);
        truSoService.saveTruSo(truSo);
        
        log.info("✅ ĐÃ LƯU TRỤ SỞ QUA API THÀNH CÔNG");
        return buildSuccessResponse(truSo, "Thêm trụ sở thành công (JSON)!");
    }
    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<String> xoaTruSo(@PathVariable Long id) {
        if (!truSoRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Trụ sở không tồn tại!");
        }
        truSoService.deleteTruSo(id);
        return ResponseEntity.ok("Xóa trụ sở thành công!");
    }

    @GetMapping("/gan-toa-do/{id}")
    public String hienThiGanToaDo(@PathVariable Long id, Model model) {
        TruSo truSo = truSoRepository.findById(id).orElse(null);
        if (truSo == null) return "redirect:/admin/quan-ly-tru-so";
        model.addAttribute("truSo", truSo);
        return "admin/gan-toa-do-tru-so";
    }

    @PostMapping("/gan-toa-do/{id}")
    @ResponseBody
    public ResponseEntity<String> ganToaDoTruSo(@PathVariable Long id,
                                                @RequestParam double kinhDo,
                                                @RequestParam double viDo) {
        try {
            TruSo truSo = truSoRepository.findById(id).orElse(null);
            if (truSo == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy trụ sở!");
            truSo.setKinhDo(kinhDo);
            truSo.setViDo(viDo);
            truSoService.saveTruSo(truSo);
            return ResponseEntity.ok("Gán tọa độ thành công!");
        } catch (Exception e) {
            log.error("Lỗi khi gán tọa độ trụ sở: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống.");
        }
    }

    // --- CÁC HÀM PHỤ TRỢ FIX LỖI SONAR (COMPLEXITY & UNBOXING) ---

    private TruSo mapToEntity(TruSoRequest request) {
        TruSo truSo = new TruSo();
        truSo.setTenTruSo(request.getTenTruSo());
        truSo.setTenDangNhap(request.getTenDangNhap());
        truSo.setMatKhau(request.getMatKhau());
        // Fix Unboxing null safety
        truSo.setKinhDo(request.getKinhDo() != null ? request.getKinhDo() : 0.0);
        truSo.setViDo(request.getViDo() != null ? request.getViDo() : 0.0);
        return truSo;
    }

    private ResponseEntity<Object> buildSuccessResponse(TruSo truSo, String message) {
        // Fix lỗi Unboxing: Dùng 0L (kiểu Long) để khớp hoàn toàn với getId()
        Long idValue = (truSo.getId() != null) ? truSo.getId() : 0L;
        String nameValue = (truSo.getTenTruSo() != null) ? truSo.getTenTruSo() : "";

        return ResponseEntity.ok(
            Map.of(
                "message", message,
                "id", idValue,
                "tenTruSo", nameValue
            )
        );
    }

    /**
     * DTO Class: Giúp bảo mật Entity và thỏa mãn quy tắc Sonar
     */
    public static class TruSoRequest {
        private String tenTruSo;
        private String tenDangNhap;
        private String matKhau;
        private Double kinhDo;
        private Double viDo;
        // Getters
        public String getTenTruSo() { return tenTruSo; }
        public String getTenDangNhap() { return tenDangNhap; }
        public String getMatKhau() { return matKhau; }
        public Double getKinhDo() { return kinhDo; }
        public Double getViDo() { return viDo; }
        // Setters
        public void setTenTruSo(String tenTruSo) { this.tenTruSo = tenTruSo; }
        public void setTenDangNhap(String tenDangNhap) { this.tenDangNhap = tenDangNhap; }
        public void setMatKhau(String matKhau) { this.matKhau = matKhau; }
        public void setKinhDo(Double kinhDo) { this.kinhDo = kinhDo; }
        public void setViDo(Double viDo) { this.viDo = viDo; }
    }
}