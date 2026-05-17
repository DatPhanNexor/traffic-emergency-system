package com.example.suco.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hoa_don")
public class HoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sosId;
    private Long trusoId;
    private String userId;

    private String tenSos; // Tên hư hỏng/sự cố
    private String noiDungXuLy; // Trụ sở đã làm gì
    
    @Column(precision = 12, scale = 2)
    private BigDecimal thanhTien;

    private Long quaId; // ID của Voucher áp dụng

    @Column(precision = 12, scale = 2)
    private BigDecimal soTienGiam; // SỐ TIỀN ĐƯỢC GIẢM

    @Column(precision = 12, scale = 2)
    private BigDecimal tongThanhToan; // GIÁ CUỐI CÙNG (Thực trả)
    
    private String trangThai; // PENDING (Chờ thanh toán), PAID (Đã thanh toán)
    private LocalDateTime createdAt;

    public HoaDon() { this.createdAt = LocalDateTime.now(); }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSosId() { return sosId; }
    public void setSosId(Long sosId) { this.sosId = sosId; }
    public Long getTrusoId() { return trusoId; }
    public void setTrusoId(Long trusoId) { this.trusoId = trusoId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTenSos() { return tenSos; }
    public void setTenSos(String tenSos) { this.tenSos = tenSos; }
    public String getNoiDungXuLy() { return noiDungXuLy; }
    public void setNoiDungXuLy(String noiDungXuLy) { this.noiDungXuLy = noiDungXuLy; }
    public BigDecimal getThanhTien() { return thanhTien; }
    public void setThanhTien(BigDecimal thanhTien) { this.thanhTien = thanhTien; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Getters and Setters cho các trường mới
    public Long getQuaId() { return quaId; }
    public void setQuaId(Long quaId) { this.quaId = quaId; }

    public BigDecimal getSoTienGiam() { return soTienGiam; }
    public void setSoTienGiam(BigDecimal soTienGiam) { this.soTienGiam = soTienGiam; }

    public BigDecimal getTongThanhToan() { return tongThanhToan; }
    public void setTongThanhToan(BigDecimal tongThanhToan) { this.tongThanhToan = tongThanhToan; }
}