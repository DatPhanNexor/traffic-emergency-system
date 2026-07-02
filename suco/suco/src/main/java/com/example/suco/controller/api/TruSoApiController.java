package com.example.suco.controller.api;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.suco.dto.TruSoMapDto;
import com.example.suco.model.TruSo;
import com.example.suco.service.TruSoService;

@RestController
@RequestMapping("/api/tru-so")
public class TruSoApiController {

    @Autowired
    private TruSoService truSoService;

    @GetMapping("/all")
    public List<TruSoMapDto> getAllTruSo() {
        return truSoService.getAllTruSoForMap();
    }

    /**
     * FIX SONAR: 
     * 1. Thay ResponseEntity<?> bằng ResponseEntity<Object>
     * 2. Thay @RequestBody TruSo bằng @RequestBody TruSoRequest (DTO)
     */
    @PostMapping
    public ResponseEntity<Object> createTruSo(@RequestBody TruSoRequest request) {
        try {
            // Chuyển đổi từ DTO sang Entity để lưu vào DB
            TruSo truSo = new TruSo();
            truSo.setTenTruSo(request.getTenTruSo());
            truSo.setTenDangNhap(request.getTenDangNhap());
            truSo.setMatKhau(request.getMatKhau());
            truSo.setKinhDo(request.getKinhDo());
            truSo.setViDo(request.getViDo());

            TruSo saved = truSoService.saveTruSo(truSo);
            
            // Trả về một Map (POJO) thay vì trả về Entity trực tiếp
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", saved.getId(),
                "tenTruSo", saved.getTenTruSo(),
                "tenDangNhap", saved.getTenDangNhap(),
                "message", "Thêm trụ sở thành công!"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * INNER DTO CLASS: Giúp bảo mật cấu trúc Database và fix lỗi Sonar
     */
    public static class TruSoRequest {
        private String tenTruSo;
        private String tenDangNhap;
        private String matKhau;
        private Double kinhDo;
        private Double viDo;

        // Getter và Setter
        public String getTenTruSo() { return tenTruSo; }
        public void setTenTruSo(String tenTruSo) { this.tenTruSo = tenTruSo; }
        public String getTenDangNhap() { return tenDangNhap; }
        public void setTenDangNhap(String tenDangNhap) { this.tenDangNhap = tenDangNhap; }
        public String getMatKhau() { return matKhau; }
        public void setMatKhau(String matKhau) { this.matKhau = matKhau; }
        public Double getKinhDo() { return kinhDo; }
        public void setKinhDo(Double kinhDo) { this.kinhDo = kinhDo; }
        public Double getViDo() { return viDo; }
        public void setViDo(Double viDo) { this.viDo = viDo; }
    }
}