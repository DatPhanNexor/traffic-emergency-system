package com.example.suco.controller.admin;

import com.example.suco.dto.GoiDto;
import com.example.suco.service.GoiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/goi")
public class AdminGoiController {

    @Autowired
    private GoiService goiService;

    @GetMapping
    public String hienThiQuanLyGoi(Model model) {
        model.addAttribute("listGoi", goiService.getAllGoi());
        model.addAttribute("goiMoi", new GoiDto()); 
        model.addAttribute("activePage", "quan-ly-goi");
        return "admin/quan-ly-goi";
    }

}