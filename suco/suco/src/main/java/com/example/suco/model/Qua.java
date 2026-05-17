package com.example.suco.model;

import java.math.BigDecimal;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qua")
public class Qua {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ten;

    @Enumerated(EnumType.STRING)
    private LoaiQua loai;

    private String moTa;
    private Integer diem;
    private String hinhAnh;
    private Integer giaTriGiamPercent; // % giảm giá
@Column(precision = 15, scale = 2) // Định nghĩa độ chính xác trong DB
    private BigDecimal giaTriToiDa;
    public enum LoaiQua {
        SAN_PHAM, VOUCHER
    }

@Column(name = "ngay_ket_thuc")
private LocalDateTime ngayKetThuc;
@Enumerated(EnumType.STRING)
private TrangThai trangThai = TrangThai.HOAT_DONG;

public enum TrangThai {
    HOAT_DONG, NGUNG
}

    public Qua() {}

    public Qua(String ten, LoaiQua loai, String moTa, Integer diem, String hinhAnh) {
        this.ten = ten;
        this.loai = loai;
        this.moTa = moTa;
        this.diem = diem;
        this.hinhAnh = hinhAnh;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTen() { return ten; }
    public void setTen(String ten) { this.ten = ten; }

    public LoaiQua getLoai() { return loai; }
    public void setLoai(LoaiQua loai) { this.loai = loai; }

    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }

    public Integer getDiem() { return diem; }
    public void setDiem(Integer diem) { this.diem = diem; }

    public String getHinhAnh() { return hinhAnh; }
    public void setHinhAnh(String hinhAnh) { this.hinhAnh = hinhAnh; }

    // Getters and Setters cho 2 trường mới
public Integer getGiaTriGiamPercent() { return giaTriGiamPercent; }
public void setGiaTriGiamPercent(Integer giaTriGiamPercent) { this.giaTriGiamPercent = giaTriGiamPercent; }

public BigDecimal getGiaTriToiDa() { return giaTriToiDa; }
public void setGiaTriToiDa(BigDecimal giaTriToiDa) { this.giaTriToiDa = giaTriToiDa; }

public LocalDateTime getNgayKetThuc() { return ngayKetThuc; }
public void setNgayKetThuc(LocalDateTime ngayKetThuc) { this.ngayKetThuc = ngayKetThuc; }

public TrangThai getTrangThai() { return trangThai; }
public void setTrangThai(TrangThai trangThai) { this.trangThai = trangThai; }
}