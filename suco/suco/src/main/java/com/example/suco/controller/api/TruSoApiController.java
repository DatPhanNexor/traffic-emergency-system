package com.example.suco.controller.api;

import java.util.List;

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

    // App Android sẽ gọi link: http://ip-cua-ban:8080/api/tru-so/all
    @GetMapping("/all")
    public List<TruSoMapDto> getAllTruSo() {
        return truSoService.getAllTruSoForMap();
    }

    /**
     * FIX BUG W4-SVP06-B01: Bổ sung API tạo trụ sở bằng JSON
     * @RequestBody giúp chuyển đổi JSON từ Postman thành đối tượng TruSo
     */
    @PostMapping
    public ResponseEntity<?> createTruSo(@RequestBody TruSo truSo) {
        try {
            TruSo saved = truSoService.saveTruSo(truSo);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}