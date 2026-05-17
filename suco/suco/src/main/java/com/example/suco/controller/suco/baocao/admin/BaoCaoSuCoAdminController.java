package com.example.suco.controller.suco.baocao.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.example.suco.service.suco.baocao.admin.AdminBaoCaoService;
import com.example.suco.service.suco.baocao.admin.DuyetSuCoService;
import com.example.suco.service.suco.baocao.user.UserBaoCaoService;
import com.example.suco.dto.SuCoMapDto;
import com.example.suco.mapper.SuCoMapDtoMapper;
import com.example.suco.model.BaoCaoSuCo;
import com.example.suco.model.Spam;
import com.example.suco.repository.BaoCaoSuCoRepository;
import com.example.suco.repository.LoaiSuCoRepository;
import com.example.suco.repository.SpamRepository;
import com.example.suco.service.BaoCaoSuCoService;

@Controller
@RequestMapping("/admin/bao-cao-su-co")
public class BaoCaoSuCoAdminController {

    @Autowired
    private BaoCaoSuCoService reportService;

    @Autowired
    private BaoCaoSuCoRepository reportRepository;

     @Autowired
    private LoaiSuCoRepository loaiSuCoRepository;

    @Autowired
    private SuCoMapDtoMapper suCoMapDtoMapper;

    @Autowired
    private SpamRepository spamRepository;

    @Autowired
    private AdminBaoCaoService adminBaoCaoService;

    @Autowired
    private DuyetSuCoService duyetSuCoService;


    @GetMapping
    public String page(Model model) {
    // 1. Lấy danh sách chờ duyệt từ bảng BaoCaoSuCo
    List<BaoCaoSuCo> listPending = reportRepository.findPendingReportsForAdmin();

    // 2. Lấy TẤT CẢ từ bảng BaoCaoSuCo
    List<BaoCaoSuCo> allSuCo = reportRepository.findByTrangThaiDuyetIn(List.of("VERIFIED"));    // 3. LẤY DỮ LIỆU TỪ BẢNG SPAM (Thay vì lấy từ reportRepository)
    List<Spam> listSpam = spamRepository.findAll(); 

    // 4. Các biến đếm
    long countCho = allSuCo.stream().filter(s -> "CHO_XU_LY".equals(s.getTrangThaiXuLy())).count();
    long countDang = allSuCo.stream().filter(s -> "DANG_XU_LY".equals(s.getTrangThaiXuLy())).count();
    long countXong = allSuCo.stream().filter(s -> "HOAN_THANH".equals(s.getTrangThaiXuLy())).count();
    
    // Đưa vào model
    model.addAttribute("listPending", listPending);
    model.addAttribute("allSuCo", allSuCo); 
    model.addAttribute("listSpam", listSpam); // Bây giờ listSpam chứa đối tượng Spam.java
    model.addAttribute("countCho", countCho);
    model.addAttribute("countDang", countDang);
    model.addAttribute("countXong", countXong);
    model.addAttribute("activePage", "bao-cao-su-co");
    return "admin/bao-cao-su-co";
    }

    @PostMapping(value = "/admin-submit", consumes = "multipart/form-data")
    @ResponseBody
    public ResponseEntity<BaoCaoSuCo> adminSubmit( // Đổi String thành BaoCaoSuCo
        @ModelAttribute BaoCaoSuCo report,
        @RequestParam("image") MultipartFile image
    ) {
        //BaoCaoSuCo saved = reportService.submitAdminReport(report, image);
        BaoCaoSuCo saved = adminBaoCaoService.submitAdminReport(report, image);
    
        // Trả về JSON cho trình duyệt xử lý
        return ResponseEntity.ok(saved); 
    }


    @PostMapping("/{id}/verify")
    @ResponseBody
    public ResponseEntity<String> verify(
            @PathVariable Long id,
            @RequestParam boolean isCorrect
    ) {
        // reportService.verifyReport(id, isCorrect);
        duyetSuCoService.verifyReport(id, isCorrect);
        return ResponseEntity.ok("Xác thực thành công!");
    }

    

    @GetMapping("/all-markers")
    @ResponseBody
    public ResponseEntity<List<BaoCaoSuCo>> getAllMarkers() {
        // Sử dụng hàm thứ hai (In) để lấy đồng thời nhiều trạng thái
        List<BaoCaoSuCo> list = reportRepository.findByTrangThaiDuyetIn(
        List.of("VERIFIED", "AI_APPROVED", "PENDING")
    );
    return ResponseEntity.ok(list);
    }

    @GetMapping("/pending")
    @ResponseBody
    public ResponseEntity<List<SuCoMapDto>> getPendingReports() {
        return ResponseEntity.ok(duyetSuCoService.getPendingReportsForAdmin());
    }
}
