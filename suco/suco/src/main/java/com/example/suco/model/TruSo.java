package com.example.suco.model;

import jakarta.persistence.*;

@Entity
@Table(name = "truso")
public class TruSo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String tenDangNhap; // tài khoản

    @Column(nullable = false)
    private String matKhau; // mật khẩu

    private String tenTruSo;

    // Vị trí hoạt động
    private double kinhDo; 
    private double viDo;   

@Column(length = 12)
private String geohash;

    public TruSo() {}

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenDangNhap() { return tenDangNhap; }
    public void setTenDangNhap(String tenDangNhap) { this.tenDangNhap = tenDangNhap; }
    public String getMatKhau() { return matKhau; }
    public void setMatKhau(String matKhau) { this.matKhau = matKhau; }
    public String getTenTruSo() { return tenTruSo; }
    public void setTenTruSo(String tenTruSo) { this.tenTruSo = tenTruSo; }
    public double getKinhDo() { return kinhDo; }
    public void setKinhDo(double kinhDo) { this.kinhDo = kinhDo; }
    public double getViDo() { return viDo; }
    
    public void setViDo(double viDo) { this.viDo = viDo; }
    public String getGeohash() { return geohash; }
public void setGeohash(String geohash) { this.geohash = geohash; }

}