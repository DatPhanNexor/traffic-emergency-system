package com.example.suco.controller.api;

import com.example.suco.service.suco.baocao.admin.DuyetSuCoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/incidents")
public class AdminIncidentApiController {

    @Autowired
    private DuyetSuCoService duyetSuCoService;

    @PostMapping("/approve")
    public ResponseEntity<?> approveIncident(@RequestBody Map<String, Object> body) {
        Object idObj = body.get("id");
        if (idObj == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu ID sự cố"));
        }

        long id;
        try {
            id = Long.parseLong(idObj.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "ID không hợp lệ"));
        }

        try {
            // Gọi logic nghiệp vụ thật dưới database!
            // Đối với API phê duyệt (approve), isCorrect mặc định là true
            duyetSuCoService.verifyReport(id, true);
            
            return ResponseEntity.ok(Map.of("message", "Thao tác phê duyệt sự cố thành công"));
        } catch (ResponseStatusException ex) {
            // Ánh xạ lỗi 404 từ Service ("Không tìm thấy báo cáo") thành "Sự cố không tồn tại"
            if (ex.getStatusCode().value() == 404) {
                return ResponseEntity.status(404).body(Map.of("message", "Sự cố không tồn tại"));
            }
            return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", ex.getReason()));
        }
    }
}
