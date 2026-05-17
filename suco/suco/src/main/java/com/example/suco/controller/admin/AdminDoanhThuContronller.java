package com.example.suco.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.suco.service.DoanhThuService;

@Controller
@RequestMapping("/admin/quan-ly-doanh-thu")
public class AdminDoanhThuContronller {
    
    @Autowired
    private DoanhThuService doanhThuService;

    @GetMapping
    public String viewDoanhThu(Model model) {
        model.addAttribute("tongDoanhThu", doanhThuService.layTongDoanhThu());
        model.addAttribute("listHoaDon", doanhThuService.layDanhSachHoaDon());
        model.addAttribute("activePage", "quan-ly-doanh-thu");
        return "admin/quan-ly-doanh-thu";
    }
}
