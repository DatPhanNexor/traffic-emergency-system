package com.example.suco.controller.api;

import com.example.suco.model.LoaiSuCo;
import com.example.suco.service.LoaiSuCoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loai-su-co")
public class LoaiSuCoApiController {

    private final LoaiSuCoService service;

    public LoaiSuCoApiController(LoaiSuCoService service) {
        this.service = service;
    }

    // Android lấy danh sách loại sự cố
    @GetMapping
    public List<LoaiSuCo> getAll() {
        return service.getLoaiSuCo();
    }
    
}
