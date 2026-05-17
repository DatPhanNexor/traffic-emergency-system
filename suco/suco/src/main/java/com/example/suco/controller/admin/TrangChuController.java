package com.example.suco.controller.admin;

import com.example.suco.model.TruSo;
import com.example.suco.repository.LoaiSuCoRepository; // Thêm import này
import com.example.suco.repository.TruSoRepository;
import com.example.suco.repository.CameraRepository;
import com.example.suco.config.AppConfig;
import com.example.suco.model.Camera; 
import org.springframework.beans.factory.annotation.Autowired; // Thêm import này
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;


@Controller
public class TrangChuController {

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private LoaiSuCoRepository loaiSuCoRepository; 
    @Autowired
    private TruSoRepository truSoRepository;
    @Autowired
    private CameraRepository cameraRepository;
    
    @GetMapping("/admin/trang-chu")
    public String trangChuAdmin(Model model) {
        model.addAttribute("mapboxToken", appConfig.getMapboxToken());
        model.addAttribute("listLoaiSuCo", loaiSuCoRepository.findAll());
        model.addAttribute("activePage", "trang-chu");
        
        // 1. Lấy các trụ sở chưa gán
        List<TruSo> listTruSoChuaGan = truSoRepository.findAll().stream()
                .filter(ts -> ts.getKinhDo() == 0 || ts.getViDo() == 0)
                .toList();
        model.addAttribute("listTruSoChuaGan", listTruSoChuaGan);

        // 2. Lấy các Camera chưa gán (Kinh độ hoặc Vĩ độ bằng 0 hoặc null)
        List<Camera> listCameraChuaGan = cameraRepository.findAll().stream()
                .filter(c -> c.getKinhDo() == null || c.getKinhDo() == 0 
                          || c.getViDo() == null || c.getViDo() == 0)
                .toList();
        model.addAttribute("listCameraChuaGan", listCameraChuaGan);
        
        return "admin/trang-chu"; 
    }
}