package com.example.suco.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
    private Double kinhDo; 
    private Double viDo;   

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
    public Double getKinhDo() { return kinhDo; }
    public void setKinhDo(Double kinhDo) { this.kinhDo = kinhDo; }
    public Double getViDo() { return viDo; }
    
    public void setViDo(Double viDo) { this.viDo = viDo; }
    public String getGeohash() { return geohash; }
public void setGeohash(String geohash) { this.geohash = geohash; }

}//