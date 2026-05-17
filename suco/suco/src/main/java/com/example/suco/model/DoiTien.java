package com.example.suco.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "doi_tien")
public class DoiTien {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private int soDiemDoi;
    private Long giaTri; // Lưu con số thuần (VD: 1000, 20000)
    private String loaiDoi; 
    private LocalDateTime ngayDoi;

    public DoiTien() {}

    public DoiTien(String userId, int soDiemDoi, Long giaTri, String loaiDoi, LocalDateTime ngayDoi) {
        this.userId = userId;
        this.soDiemDoi = soDiemDoi;
        this.giaTri = giaTri;
        this.loaiDoi = loaiDoi;
        this.ngayDoi = ngayDoi;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getSoDiemDoi() { return soDiemDoi; }
    public void setSoDiemDoi(int soDiemDoi) { this.soDiemDoi = soDiemDoi; }
    public Long getGiaTri() { return giaTri; }
    public void setGiaTri(Long giaTri) { this.giaTri = giaTri; }
    public String getLoaiDoi() { return loaiDoi; }
    public void setLoaiDoi(String loaiDoi) { this.loaiDoi = loaiDoi; }
    public LocalDateTime getNgayDoi() { return ngayDoi; }
    public void setNgayDoi(LocalDateTime ngayDoi) { this.ngayDoi = ngayDoi; }
}