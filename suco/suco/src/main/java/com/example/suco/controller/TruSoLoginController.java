package com.example.suco.controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.suco.config.AppConfig;
import com.example.suco.model.TinHieuSOS;
import com.example.suco.model.TruSo;
import com.example.suco.repository.TinHieuSOSRepository;
import com.example.suco.repository.TruSoRepository;
import java.util.Map;
import java.util.List;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
@Controller
@RequestMapping("/truso")
public class TruSoLoginController {

    private final TruSoRepository truSoRepository;
    private final TinHieuSOSRepository tinHieuSOSRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AppConfig appConfig;
    public TruSoLoginController(TruSoRepository truSoRepository,
                             TinHieuSOSRepository tinHieuSOSRepository,
                             AppConfig appConfig) {
    this.truSoRepository = truSoRepository;
    this.tinHieuSOSRepository = tinHieuSOSRepository;
    this.appConfig = appConfig;
}

    @GetMapping("/login")
public String trangLogin() {
    return "truso/login";
}


    // @PostMapping("/login")
    // public String xuLyLogin(@RequestParam String username, 
    //                         @RequestParam String password, 
    //                         HttpSession session, 
    //                         Model model) {
    //     Optional<TruSo> truSo = truSoRepository.findByTenDangNhap(username);
    //     if (truSo.isPresent() && passwordEncoder.matches(password, truSo.get().getMatKhau())) {
    //         // Lưu vào session để dùng ở trang chủ và chặn truy cập trái phép
    //         session.setAttribute("currentTruSo", truSo.get());
    //         return "redirect:/truso/trang-chu";
    //     }
    //     model.addAttribute("error", "Tài khoản hoặc mật khẩu không đúng!");
    //     return "truso/login";
    // }

    @PostMapping("/login")
@ResponseBody
public ResponseEntity<?> login(@RequestParam String username,
                               @RequestParam String password,
                               HttpSession session) {

    Optional<TruSo> truSo = truSoRepository.findByTenDangNhap(username);

    if (truSo.isPresent() && passwordEncoder.matches(password, truSo.get().getMatKhau())) {

        session.setAttribute("currentTruSo", truSo.get());

        TruSo t = truSo.get();
         session.setAttribute("currentTruSo", t);

    var auth = new UsernamePasswordAuthenticationToken(
                t, // principal
                null,
                List.of() // nếu chưa dùng role
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        return ResponseEntity.ok(Map.of(
                "message", "Login success",
                "id", t.getId(),
                "tenTruSo", t.getTenTruSo(),
                "tenDangNhap", t.getTenDangNhap()
        ));
    }
           

    return ResponseEntity.status(401).body(Map.of(
            "message", "Sai tài khoản hoặc mật khẩu"
    ));
}

   @GetMapping("/trang-chu")
public String trangChu(HttpSession session, Model model) {

    if (session.getAttribute("currentTruSo") == null) {
        return "redirect:/truso/login";
    }

    model.addAttribute("mapboxToken", appConfig.getMapboxToken());

    return "truso/trang-chu";
}
    @GetMapping("/quan-ly-cuu-tro")
    public String quanLyCuuTro(HttpSession session) {
        if (session.getAttribute("currentTruSo") == null) {
            return "redirect:/truso/login";
        }
        return "truso/quan-ly-cuu-tro";
    }

    @GetMapping("/lich-su-cuu-tro")
    public String lichSuCuuTro(HttpSession session, Model model) {
        if (session.getAttribute("currentTruSo") == null) {
            return "redirect:/truso/login";
        }
        Object cs = session.getAttribute("currentTruSo");
        java.util.List<TinHieuSOS> lichSu = java.util.Collections.emptyList();
        try {
            if (cs instanceof com.example.suco.model.TruSo) {
                com.example.suco.model.TruSo current = (com.example.suco.model.TruSo) cs;
                // Lấy các SOS đã hoàn thành cho trụ sở này
                lichSu = tinHieuSOSRepository.findByIdTruSoTiepNhanAndTrangThai(current.getId(), "HOAN_THANH");
            }
        } catch (Exception e) {
            // avoid throwing to template; log to stdout for troubleshooting
            System.err.println("Error loading lich su cuu tro: " + e.getMessage());
            e.printStackTrace();
            lichSu = java.util.Collections.emptyList();
        }
        model.addAttribute("lichSuList", lichSu);
        return "truso/lich-su-cuu-tro";
    }
    @GetMapping("/dang-cuu-tro")
    public String dangCuuTro(HttpSession session) {
        if (session.getAttribute("currentTruSo") == null) {
            return "redirect:/truso/login";
        }
        return "truso/dang-cuu-tro";
    }
    @GetMapping("/api/sos-cua-toi")
@ResponseBody
public List<TinHieuSOS> sosCuaToi(HttpSession session) {
    TruSo current = (TruSo) session.getAttribute("currentTruSo");
    if (current == null) return List.of();

    return tinHieuSOSRepository.findActiveByTruSo(current.getId());
}

}