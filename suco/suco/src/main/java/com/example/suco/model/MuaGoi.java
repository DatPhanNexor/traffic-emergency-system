package com.example.suco.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mua_goi")
public class MuaGoi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "goi_id")
    private Long goiId;

    @Column(name = "ngay_mua")
    private LocalDateTime ngayMua;


    @Column(name = "ngay_het_han") // Thêm cột này
    private LocalDateTime ngayHetHan;

    @Column(name = "trang_thai")
    private String trangThai;

    public MuaGoi() {
        this.ngayMua = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public Long getGoiId() { return goiId; }
    public LocalDateTime getNgayMua() { return ngayMua; }
    public LocalDateTime getNgayHetHan() { return ngayHetHan; }
    public String getTrangThai() { return trangThai; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setGoiId(Long goiId) { this.goiId = goiId; }
    public void setNgayMua(LocalDateTime ngayMua) { this.ngayMua = ngayMua; }
        public void setNgayHetHan(LocalDateTime ngayHetHan) { this.ngayHetHan = ngayHetHan; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
}