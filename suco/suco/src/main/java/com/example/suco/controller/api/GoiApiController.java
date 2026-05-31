package com.example.suco.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.suco.dto.GoiDto;
import com.example.suco.model.Goi;
import com.example.suco.service.GoiService;

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
}//
