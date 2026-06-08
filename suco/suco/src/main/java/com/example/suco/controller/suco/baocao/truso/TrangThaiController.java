package com.example.suco.controller.suco.baocao.truso;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.suco.dto.SuCoMapDto;
import com.example.suco.model.BaoCaoSuCo;
import com.example.suco.model.TruSo;
import com.example.suco.repository.BaoCaoSuCoRepository;
import com.example.suco.service.suco.baocao.truso.TrangThaiService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping
public class TrangThaiController {

    @Autowired
    private BaoCaoSuCoRepository repo;

    @Autowired
    private TrangThaiService trangThaiServiceService;

    @GetMapping("/su-co/danh-sach-hien-tai")
    public ResponseEntity<?> getSuCoHienTai(
            @RequestParam(required = false) String status,
            HttpSession session) {

        TruSo current = (TruSo) session.getAttribute("currentTruSo");
        if (current == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Vui lòng đăng nhập tài khoản trụ sở!"));
        }

        List<SuCoMapDto> allActive =
                repo.findActiveByTruSo(current.getId());

        if (status != null && !status.isEmpty()) {
            List<SuCoMapDto> filtered = allActive.stream()
                    .filter(s -> s.getTrangThaiXuLy() != null
                            && s.getTrangThaiXuLy().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(filtered);
        }
        return ResponseEntity.ok(allActive);
    }

  
    @GetMapping("/su-co/lich-su")
    public List<BaoCaoSuCo> getSuCoHistory(HttpSession session) {

        TruSo current = (TruSo) session.getAttribute("currentTruSo");
        if (current == null) return List.of();

        return repo.findHistoryByTruSo(current.getId());
    }

    @PatchMapping("/su-co/cap-nhat-trang-thai/{id}")
    public ResponseEntity<?> updateSuCoStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        String status = body.get("status");
        TruSo current = (TruSo) session.getAttribute("currentTruSo");
        if (current == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Vui lòng đăng nhập tài khoản trụ sở!"));
        }
        Map<String, Object> result =
                trangThaiServiceService.updateSuCoStatus(id, status, current);
        return ResponseEntity.ok(result);
    }

    // Alias endpoint for Postman Week 3 / SVP-04 API
    @PatchMapping("/api/map/su-co/cap-nhat-trang-thai/{id}")
    public ResponseEntity<?> updateSuCoStatusMapApi(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        return updateSuCoStatus(id, body, session);
    }

}