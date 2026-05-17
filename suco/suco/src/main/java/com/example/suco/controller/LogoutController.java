package com.example.suco.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.Map;
import jakarta.servlet.http.Cookie;

@Controller
public class LogoutController {

@PostMapping("/truso/logout")
@ResponseBody
public ResponseEntity<?> logout(HttpSession session, HttpServletResponse response) {

    session.invalidate();

    Cookie cookie = new Cookie("JSESSIONID", null);
    cookie.setMaxAge(0);
    cookie.setPath("/");
    response.addCookie(cookie);

    return ResponseEntity.ok(Map.of("message", "Logout success"));
}
}
