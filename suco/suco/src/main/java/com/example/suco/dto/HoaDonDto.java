package com.example.suco.dto;

public class HoaDonDto {
    private Long sosId;
    private String tenSos;
    private Double thanhTien; // Giá gốc
    private Double soTienGiam; // Số tiền được giảm
    private Double tongThanhToan; // Giá cuối cùng
    private Long quaId; // ID quà nếu có
    private String xuLy;
    private Double giaThuCong;
    private Long trusoId;

    // Getters và Setters
    public Long getSosId() { return sosId; }
    public void setSosId(Long sosId) { this.sosId = sosId; }
    public String getTenSos() { return tenSos; }
    public void setTenSos(String tenSos) { this.tenSos = tenSos; }
    public Double getThanhTien() { return thanhTien; }
    public void setThanhTien(Double thanhTien) { this.thanhTien = thanhTien; }
    public Double getSoTienGiam() { return soTienGiam; }
    public void setSoTienGiam(Double soTienGiam) { this.soTienGiam = soTienGiam; }
    public Double getTongThanhToan() { return tongThanhToan; }
    public void setTongThanhToan(Double tongThanhToan) { this.tongThanhToan = tongThanhToan; }
    public Long getQuaId() { return quaId; }
    public void setQuaId(Long quaId) { this.quaId = quaId; }
    public String getXuLy() { return xuLy; }
    public void setXuLy(String xuLy) { this.xuLy = xuLy; }
    public Double getGiaThuCong() { return giaThuCong; }
    public void setGiaThuCong(Double giaThuCong) { this.giaThuCong = giaThuCong; }
    public Long getTrusoId() { return trusoId; }
    public void setTrusoId(Long trusoId) { this.trusoId = trusoId; }
}