package com.example.suco.controller.admin;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.example.suco.model.LoaiSuCo;
import com.example.suco.service.LoaiSuCoService;

@Controller
@RequestMapping("/admin/loai-su-co")
public class LoaiSuCoAdminController {

    private final LoaiSuCoService service;

    public LoaiSuCoAdminController(LoaiSuCoService service) {
        this.service = service;
    }

    // MỞ TRANG QUẢN LÝ
    @GetMapping
    public String page(Model model) {
        model.addAttribute("list", service.getLoaiSuCo());
        model.addAttribute("activePage", "loai-su-co");
        return "admin/loai-su-co"; // trỏ tới loai-su-co.html
    }

@PostMapping(value = {"", "/api/create"})
@ResponseBody
public ResponseEntity<?> createApi(
        @RequestParam String ten,
        @RequestParam(value = "iconFile", required = false) MultipartFile file
) throws IOException {

    LoaiSuCo saved = service.createLoaiSuCo(ten, file);
    return ResponseEntity.ok(saved);
}
@DeleteMapping("/{id}")
@ResponseBody
public ResponseEntity<?> delete(@PathVariable Long id) {
    service.deleteLoaiSuCo(id);
    return ResponseEntity.ok(Map.of("message", "Xóa thành công"));
}
@PatchMapping("/{id}")
@ResponseBody
public ResponseEntity<?> updateApi(
        @PathVariable Long id,
        @RequestParam String ten,
        @RequestParam(value = "iconFile", required = false) MultipartFile file
) throws IOException {
    LoaiSuCo updated = service.updateLoaiSuCo(id, ten, file);
    return ResponseEntity.ok(updated);
}
}
