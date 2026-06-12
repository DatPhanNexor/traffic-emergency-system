package com.example.suco.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/truso")
public class TruSoLoginController {

    private final TruSoRepository truSoRepository;
    private final TinHieuSOSRepository tinHieuSOSRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AppConfig appConfig;
    private static final String MESSAGE = "message";

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

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> login(@RequestParam(required = false) String username,
                                   @RequestParam(required = false) String password,
                                   HttpSession session) {

        // --- ĐOẠN KIỂM TRA ĐẦU VÀO (VALIDATION) ---
        // Kiểm tra username null hoặc chỉ chứa khoảng trắng
        if (username == null || username.trim().isEmpty()) {
            session.invalidate();
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(400).body(Map.of(
                    MESSAGE, "Tên đăng nhập không được để trống"
            ));
        }

        // Kiểm tra password null hoặc chỉ chứa khoảng trắng
        if (password == null || password.trim().isEmpty()) {
            session.invalidate();
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(400).body(Map.of(
                    MESSAGE, "Mật khẩu không được để trống"
            ));
        }

        // Chỉ kiểm tra DB sau khi dữ liệu đầu vào đã hợp lệ
        Optional<TruSo> truSo = truSoRepository.findByTenDangNhap(username);

        if (truSo.isPresent() && passwordEncoder.matches(password, truSo.get().getMatKhau())) {

            TruSo t = truSo.get();
            session.setAttribute("currentTruSo", t);

            var auth = new UsernamePasswordAuthenticationToken(
                    t, // principal
                    null,
                    List.of() // nếu chưa dùng role
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            return ResponseEntity.ok(Map.of(
                    MESSAGE, "Login success",
                    "id", t.getId(),
                    "tenTruSo", t.getTenTruSo(),
                    "tenDangNhap", t.getTenDangNhap()
            ));
        }

        // Hủy session hiện tại và xóa toàn bộ dữ liệu đăng nhập cũ nếu thông tin sai
        session.invalidate();
        SecurityContextHolder.clearContext();

        return ResponseEntity.status(401).body(Map.of(
                MESSAGE, "Tài khoản hoặc mật khẩu không đúng!"
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
                lichSu = tinHieuSOSRepository.findByIdTruSoTiepNhanAndTrangThai(current.getId(), "HOAN_THANH");
            }
        } catch (Exception e) {
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

    @GetMapping("/api/session")
    @ResponseBody
    public ResponseEntity<?> getCurrentSession(HttpSession session) {
        Object current = session.getAttribute("currentTruSo");

        if (current == null) {
            return ResponseEntity.status(401).body(Map.of(
                    MESSAGE, "Trụ sở chưa đăng nhập"
            ));
        }

        TruSo t = (TruSo) current;
        return ResponseEntity.ok(Map.of(
                "id", t.getId(),
                "tenTruSo", t.getTenTruSo(),
                "tenDangNhap", t.getTenDangNhap()
        ));
    }
}