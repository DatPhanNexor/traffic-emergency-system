package com.example.suco.controller.api;

import com.example.suco.dto.CameraMapDto;
import com.example.suco.service.CameraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.suco.model.BaoCaoSuCo;
import com.example.suco.repository.BaoCaoSuCoRepository;
import java.util.List;

@RestController
@RequestMapping("/api/camera")
public class CameraApiController {

    @Autowired
    private CameraService cameraService;

    @Autowired
private BaoCaoSuCoRepository baoCaoSuCoRepository;

    @GetMapping("/all")
    public List<CameraMapDto> getAllCamera() {
        return cameraService.getAllCameraForMap();
    }
   @GetMapping("/near-by-incident/{id}")
public List<CameraMapDto> getCameraByIncident(@PathVariable Long id) {

        System.out.println("🔥 API CALLED: near-by-incident " + id);

    BaoCaoSuCo report = baoCaoSuCoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sự cố"));

    return cameraService.getCamerasNearIncident(
            report.getViDo(),
            report.getKinhDo()
    );
}
}