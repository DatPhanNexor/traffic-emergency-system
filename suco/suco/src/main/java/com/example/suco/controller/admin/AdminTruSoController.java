package com.example.suco.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
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

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@Controller
@RequestMapping("/admin/quan-ly-tru-so")
public class AdminTruSoController {

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

 @PostMapping(
    value = "/them",
    consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
)
@ResponseBody
public ResponseEntity<?> themTruSo(@ModelAttribute TruSo truSo,
                                    HttpServletRequest request) {

    System.out.println("🔥 ===== VÀO CONTROLLER /them TRU SO =====");
    System.out.println("📌 Content-Type: " + request.getContentType());

    System.out.println("📌 tenDangNhap: " + truSo.getTenDangNhap());
    System.out.println("📌 tenTruSo: " + truSo.getTenTruSo());
    System.out.println("📌 kinhDo: " + truSo.getKinhDo());
    System.out.println("📌 viDo: " + truSo.getViDo());

    truSoService.saveTruSo(truSo);

    System.out.println("✅ ĐÃ LƯU TRỤ SỞ ID = " + truSo.getId());

    return ResponseEntity.ok(
        java.util.Map.of(
            "message", "Thêm trụ sở thành công!",
            "id", truSo.getId(),
            "tenDangNhap", truSo.getTenDangNhap(),
            "tenTruSo", truSo.getTenTruSo(),
            "kinhDo", truSo.getKinhDo(),
            "viDo", truSo.getViDo(),
            "geohash", truSo.getGeohash()
        )
    );
}
@DeleteMapping("/delete/{id}")
@ResponseBody
public ResponseEntity<String> xoaTruSo(@PathVariable Long id) {
    if (!truSoRepository.existsById(id)) {
        return ResponseEntity.status(404).body("Trụ sở không tồn tại!");
    }

    truSoService.deleteTruSo(id);
    return ResponseEntity.ok("Xóa trụ sở thành công!");
}

    @GetMapping("/gan-toa-do/{id}")
    public String hienThiGanToaDo(@PathVariable Long id, Model model) {
        TruSo truSo = truSoRepository.findById(id).orElse(null);
        if (truSo == null) {
            model.addAttribute("error", "Không tìm thấy trụ sở!");
            return "redirect:/admin/quan-ly-tru-so";
        }
        model.addAttribute("truSo", truSo);
        return "admin/gan-toa-do-tru-so"; // Tạo file này ở templates/admin/
    }

    @PostMapping("/gan-toa-do/{id}")
    @ResponseBody
    public ResponseEntity<String> ganToaDoTruSo(@PathVariable Long id,
                                                @RequestParam double kinhDo,
                                                @RequestParam double viDo) {
        try {
            TruSo truSo = truSoRepository.findById(id).orElse(null);
            if (truSo == null) return ResponseEntity.status(404).body("Không tìm thấy trụ sở!");
            truSo.setKinhDo(kinhDo);
            truSo.setViDo(viDo);
            truSoService.saveTruSo(truSo);
            return ResponseEntity.ok("Gán tọa độ thành công!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
        }
    }
}