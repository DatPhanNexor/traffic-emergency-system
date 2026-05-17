package com.example.suco.controller.suco.baocao.truso;
import com.example.suco.model.TruSo;
import com.example.suco.service.suco.baocao.truso.MucDoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping
public class MucDoController {

    @Autowired
    private MucDoService mucDoService;

    @PatchMapping("/su-co/cap-nhat-muc-do/{id}")
    public ResponseEntity<?> capNhatMucDo(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpSession session) {

        String mucDo = body.get("mucDo");

        TruSo current = (TruSo) session.getAttribute("currentTruSo");
        if (current == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Vui lòng đăng nhập!"));
        }

        Map<String, Object> result =
                mucDoService.capNhatMucDo(id, mucDo, current);

        return ResponseEntity.ok(result);
    }
}