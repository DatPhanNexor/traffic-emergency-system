package com.example.suco.controller.api;

import com.example.suco.dto.GoiDto;
import com.example.suco.service.GoiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.suco.model.Goi;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goi")
public class GoiApiController {

    @Autowired
    private GoiService goiService;

    @GetMapping("/danh-sach")
    public ResponseEntity<?> getDanhSachGoi() {
        return ResponseEntity.ok(goiService.getAllGoi());
    }

    // 🔹 Tạo
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createGoi(@RequestBody GoiDto dto) {
        Goi goi = goiService.createGoi(dto);
        return ResponseEntity.ok(goi);
    }

    // 🔹 Xóa
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteGoi(@PathVariable Long id) {
        goiService.deleteGoi(id);
        return ResponseEntity.ok("Xóa thành công");
    }

    // 🔹 Update (PATCH hoặc POST đều được)
    @PatchMapping("/update/{id}")
public ResponseEntity<?> updateGoi(@PathVariable Long id,
                                   @RequestBody GoiDto dto) {
    goiService.updateGoi(id, dto);
    return ResponseEntity.ok("Cập nhật thành công");
}
}
