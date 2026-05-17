package com.example.suco.dto;

import java.math.BigDecimal;

public class GoiDto {
    private Long id;
    private String ten;
    private Integer thoiHan;
    private BigDecimal gia;
    private Integer khoangCachMienPhi;
    private String uuDai;

    public GoiDto() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTen() { return ten; }
    public void setTen(String ten) { this.ten = ten; }

    public Integer getThoiHan() { return thoiHan; }
    public void setThoiHan(Integer thoiHan) { this.thoiHan = thoiHan; }

    public BigDecimal getGia() { return gia; }
    public void setGia(BigDecimal gia) { this.gia = gia; }

    public Integer getKhoangCachMienPhi() { return khoangCachMienPhi; }
    public void setKhoangCachMienPhi(Integer khoangCachMienPhi) { this.khoangCachMienPhi = khoangCachMienPhi; }

    public String getUuDai() { return uuDai; }
    public void setUuDai(String uuDai) { this.uuDai = uuDai; }
}