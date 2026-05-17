package com.example.suco.controller.admin;

import com.example.suco.model.User;
import com.example.suco.service.UserService;
import com.example.suco.repository.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/quan-ly-user")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    // ===================== HTML PAGE =====================
    @GetMapping
    public String index(Model model,
                        @RequestParam(required = false) String keyword) {

        if (keyword != null && !keyword.isBlank()) {
            model.addAttribute("listUsers", userService.searchUsers(keyword));
            model.addAttribute("keyword", keyword);
        } else {
            model.addAttribute("listUsers", userService.getAllUsers());
        }

            model.addAttribute("activePage", "quan-ly-user");


        return "admin/quan-ly-user";
    }

    // ===================== JSON API =====================
    @GetMapping("/danh-sach")
    @ResponseBody
    public ResponseEntity<List<User>> getAllUsersApi() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // ===================== DELETE USER =====================
    @DeleteMapping("/delete/{uid}")
    @ResponseBody
    public ResponseEntity<?> deleteUserApi(@PathVariable String uid) {

        if (!isValidUid(uid)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_UID",
                    "message", "Uid không hợp lệ hoặc quá dài."
            ));
        }

        User existingUser = userRepository.findById(uid).orElse(null);
        if (existingUser == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "USER_NOT_FOUND",
                    "message", "Không tìm thấy user với uid: " + uid
            ));
        }

        userService.deleteUser(uid);

        return ResponseEntity.ok(Map.of(
                "message", "Xóa user thành công",
                "deletedUser", existingUser
        ));
    }

    // ===================== RESET SPAM =====================
    @GetMapping("/reset-spam/{uid}")
    public String resetSpam(@PathVariable String uid) {
        userService.resetSpamCount(uid);
        return "redirect:/admin/quan-ly-user";
    }

    // ===================== VALIDATION =====================
    private boolean isValidUid(String uid) {
        return uid != null
                && uid.length() <= 255
                && uid.matches("^[A-Za-z0-9_-]+$");
    }
}