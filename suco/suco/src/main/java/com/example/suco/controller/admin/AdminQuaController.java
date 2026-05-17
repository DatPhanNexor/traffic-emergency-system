package com.example.suco.controller.admin;

import com.example.suco.dto.QuaDto;
import com.example.suco.model.Qua;
import com.example.suco.service.QuaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;


@Controller
@RequestMapping("/admin/quan-ly-qua")
public class AdminQuaController {
    @Autowired
    private QuaService quaService;

    @Autowired
    private com.example.suco.repository.QuaRepository quaRepository;


    @GetMapping
    public String listQua(Model model) {
        model.addAttribute("listQua", quaService.getAllQua());
        model.addAttribute("quaDto", new QuaDto());
        model.addAttribute("loaiQua", Qua.LoaiQua.values());
        model.addAttribute("activePage", "quan-ly-qua");

        return "admin/quan-ly-qua";
    }

    @GetMapping("/all")
@ResponseBody
public List<Qua> getAllQuaApi() {
return quaRepository.findAll();
}

    @PostMapping("/add")
@ResponseBody
public ResponseEntity<?> addQua(@ModelAttribute QuaDto quaDto) {
    try {
        quaService.addQua(quaDto);

        return ResponseEntity.ok(Map.of(
                "code", "SUCCESS",
                "message", "Thêm quà thành công"
        ));

    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of(
                "code", "ERROR",
                "message", e.getMessage()
        ));
    }
}

    // @GetMapping("/delete/{id}")
    // public String deleteQua(@PathVariable Long id) {
    //     quaService.deleteQua(id);
    //     return "redirect:/admin/quan-ly-qua";
    // }
@DeleteMapping("/delete/{id}")
public ResponseEntity<?> deleteQua(@PathVariable Long id) {
    try {
        quaService.deleteQua(id);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Xóa thành công"
        ));
    } catch (Exception e) {
        return ResponseEntity.status(404).body(Map.of(
            "status", "error",
            "message", "Không tìm thấy quà"
        ));
    }
}
// @PutMapping("/edit/{id}")
// @ResponseBody
// public ResponseEntity<?> editQua(
//         @PathVariable Long id,
//         @RequestBody QuaDto dto
// ) {
//     try {
//         quaService.updateQua(id, dto);
//         return ResponseEntity.ok("Cập nhật thành công");
//     } catch (Exception e) {
//         return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
//     }
// }

@PatchMapping("/edit/{id}")
@ResponseBody
public ResponseEntity<?> editQua(
        @PathVariable Long id,
        @ModelAttribute QuaDto dto 
) {
    System.out.println("DTO ngayKetThuc = " + dto.getNgayKetThuc());
    try {
        quaService.updateQua(id, dto);
        return ResponseEntity.ok("Cập nhật thành công");
    } catch (Exception e) {
        return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
    }
}

// @PutMapping("/status/{id}")
// @ResponseBody
// public ResponseEntity<?> updateStatus(
//         @PathVariable Long id,
//         @RequestParam Qua.TrangThai trangThai
// ) {
//     try {
//         quaService.updateStatus(id, trangThai);
//         return ResponseEntity.ok("OK");
//     } catch (Exception e) {
//         e.printStackTrace();
//         return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
//     }
// }
}
