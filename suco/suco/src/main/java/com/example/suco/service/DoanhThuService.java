package com.example.suco.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.suco.model.HoaDon;
import com.example.suco.repository.DoanhThuRepository;

@Service
public class DoanhThuService {
    
    @Autowired
    private DoanhThuRepository doanhThuRepository;

    public BigDecimal layTongDoanhThu() {
        return doanhThuRepository.getTongDoanhThu();
    }

    public List<HoaDon> layDanhSachHoaDon() {
        return doanhThuRepository.getDanhSachHoaDon();
    }
}
