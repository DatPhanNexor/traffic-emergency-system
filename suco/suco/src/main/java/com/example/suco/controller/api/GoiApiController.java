package com.example.suco.controller.api;

import com.example.suco.dto.GoiDto;
import com.example.suco.model.Goi;
import com.example.suco.service.GoiService;
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

@RestController
@RequestMapping("/api/goi")
public class GoiApiController {

    @Autowired
    private GoiService goiService;

    @GetMapping("/danh-sach")
    public ResponseEntity<Object> getDanhSachGoi() {
        return ResponseEntity.ok(goiService.getAllGoi());
    }

    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<Object> createGoi(@RequestBody GoiDto dto) {
        Goi goi = goiService.createGoi(dto);
        return ResponseEntity.ok(goi);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Object> deleteGoi(@PathVariable Long id) {
        goiService.deleteGoi(id);
        return ResponseEntity.ok("Xóa thành công");
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<Object> updateGoi(
            @PathVariable Long id,
            @RequestBody GoiDto dto
    ) {
        Goi goi = goiService.updateGoi(id, dto);
        return ResponseEntity.ok(goi);
    }
}