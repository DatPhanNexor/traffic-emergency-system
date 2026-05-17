// package com.example.suco.controller.suco.baocao.truso;

// import com.example.suco.dto.SuCoMapDto;
// import com.example.suco.model.BaoCaoSuCo;
// import com.example.suco.model.TinHieuSOS;
// import com.example.suco.model.TruSo;
// import com.example.suco.repository.BaoCaoSuCoRepository;
// import com.example.suco.repository.TinHieuSOSRepository;
// import com.example.suco.service.BaoCaoSuCoService;
// import com.example.suco.service.DieuPhoiSOSService;
// import com.example.suco.service.suco.baocao.truso.MucDoService;
// import com.example.suco.service.suco.baocao.truso.TrangThaiService;

// import jakarta.servlet.http.HttpSession;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.messaging.simp.SimpMessagingTemplate;
// import org.springframework.web.bind.annotation.*;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.Map;
// import java.util.stream.Collectors;

// @RestController
// @RequestMapping("/api/tru-so")
// public class BaoCaoSuCoTruSoController {

//     @Autowired
//     private BaoCaoSuCoService baoCaoSuCoService;

//     @Autowired
//     private BaoCaoSuCoRepository repo;

//     @Autowired
//     private SimpMessagingTemplate messagingTemplate;

//     @Autowired
//     private MucDoService mucDoService;

//     @Autowired
//     private TrangThaiService tiepNhanService;

//     @GetMapping("/su-co/danh-sach-hien-tai")
//     public List<SuCoMapDto> getSuCoHienTai(
//             @RequestParam(required = false) String status,
//             HttpSession session) {

//         TruSo current = (TruSo) session.getAttribute("currentTruSo");
//         if (current == null) return List.of();

//         List<SuCoMapDto> allActive =
//                 repo.findActiveByTruSo(current.getId());

//         if (status != null && !status.isEmpty()) {
//             return allActive.stream()
//                     .filter(s -> s.getTrangThaiXuLy() != null
//                             && s.getTrangThaiXuLy().equalsIgnoreCase(status))
//                     .collect(Collectors.toList());
//         }
//         return allActive;
//     }

  
//     @GetMapping("/su-co/lich-su")
//     public List<BaoCaoSuCo> getSuCoHistory(HttpSession session) {

//         TruSo current = (TruSo) session.getAttribute("currentTruSo");
//         if (current == null) return List.of();

//         return repo.findHistoryByTruSo(current.getId());
//     }

//     @PatchMapping("/su-co/cap-nhat-trang-thai/{id}")
//     public ResponseEntity<?> updateSuCoStatus(
//             @PathVariable Long id,
//             @RequestBody Map<String, String> body,
//             HttpSession session) {
//         String status = body.get("status");
//         TruSo current = (TruSo) session.getAttribute("currentTruSo");
//         if (current == null) {
//             return ResponseEntity.status(401)
//                     .body(Map.of("message", "Vui lòng đăng nhập tài khoản trụ sở!"));
//         }
//         Map<String, Object> result =
//                 baoCaoSuCoService.updateSuCoStatus(id, status, current);
//         return ResponseEntity.ok(result);
//     }

//     @PatchMapping("/su-co/cap-nhat-muc-do/{id}")
//     public ResponseEntity<?> capNhatMucDo(
//             @PathVariable Long id,
//             @RequestBody Map<String, String> body,
//             HttpSession session) {

//         String mucDo = body.get("mucDo");

//         TruSo current = (TruSo) session.getAttribute("currentTruSo");
//         if (current == null) {
//             return ResponseEntity.status(401)
//                     .body(Map.of("message", "Vui lòng đăng nhập!"));
//         }

//         Map<String, Object> result =
//                 baoCaoSuCoService.capNhatMucDo(id, mucDo, current);

//         return ResponseEntity.ok(result);
//     }
// }