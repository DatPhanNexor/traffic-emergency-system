package com.example.suco.dto;

import java.util.List;
import java.util.Map;

public class ThongKeQuyDto {
    private long tongGiaTri; // Tổng số tiền trong quỹ
    private List<Map<String, Object>> lichSuVinhDanh;

    public ThongKeQuyDto() {}
    public ThongKeQuyDto(long tongGiaTri, List<Map<String, Object>> lichSuVinhDanh) {
        this.tongGiaTri = tongGiaTri;
        this.lichSuVinhDanh = lichSuVinhDanh;
    }

    // Getters and Setters
    public long getTongGiaTri() { return tongGiaTri; }
    public void setTongGiaTri(long tongGiaTri) { this.tongGiaTri = tongGiaTri; }
    
    public List<Map<String, Object>> getLichSuVinhDanh() { return lichSuVinhDanh; }
    public void setLichSuVinhDanh(List<Map<String, Object>> lichSuVinhDanh) { this.lichSuVinhDanh = lichSuVinhDanh; }
}