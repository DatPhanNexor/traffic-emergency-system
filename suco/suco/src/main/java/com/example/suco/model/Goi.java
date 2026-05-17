package com.example.suco.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "goi")
public class Goi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ten;

    @Column(name = "thoi_han")
    private Integer thoiHan;

    @Column(precision = 12, scale = 2)
    private BigDecimal gia;

    @Column(name = "khoang_cach_mien_phi")
    private Integer khoangCachMienPhi;

    @Column(columnDefinition = "TEXT")
    private String uuDai;

    public Goi() {}

    public Goi(Long id, String ten, Integer thoiHan, BigDecimal gia, Integer khoangCachMienPhi, String uuDai) {
        this.id = id;
        this.ten = ten;
        this.thoiHan = thoiHan;
        this.gia = gia;
        this.khoangCachMienPhi = khoangCachMienPhi;
        this.uuDai = uuDai;
    }

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