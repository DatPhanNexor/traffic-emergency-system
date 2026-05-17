package com.example.suco.dto;

import com.example.suco.model.Qua;
import com.example.suco.model.Qua.LoaiQua;
import com.fasterxml.jackson.annotation.JsonFormat;

import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
public class QuaDto {
    private String ten;
    private LoaiQua loai;
    private String moTa;

    
    private Integer diem;
    private MultipartFile hinhAnh;
    private Integer giaTriGiamPercent;
private java.math.BigDecimal giaTriToiDa;
@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
private LocalDateTime ngayKetThuc;

private Qua.TrangThai trangThai;

    public QuaDto() {}

    // Getters and Setters
    public String getTen() { return ten; }
    public void setTen(String ten) { this.ten = ten; }

    public LoaiQua getLoai() { return loai; }
    public void setLoai(LoaiQua loai) { this.loai = loai; }

    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }

    public Integer getDiem() { return diem; }
    public void setDiem(Integer diem) { this.diem = diem; }

    public MultipartFile getHinhAnh() { return hinhAnh; }
    public void setHinhAnh(MultipartFile hinhAnh) { this.hinhAnh = hinhAnh; }
    
    public Integer getGiaTriGiamPercent() { return giaTriGiamPercent; }
    public void setGiaTriGiamPercent(Integer giaTriGiamPercent) { this.giaTriGiamPercent = giaTriGiamPercent; }
    
    public java.math.BigDecimal getGiaTriToiDa() { return giaTriToiDa; }
public void setGiaTriToiDa(java.math.BigDecimal giaTriToiDa) { this.giaTriToiDa = giaTriToiDa; }


public LocalDateTime getNgayKetThuc() { return ngayKetThuc; }
public void setNgayKetThuc(LocalDateTime ngayKetThuc) { this.ngayKetThuc = ngayKetThuc; }

public Qua.TrangThai getTrangThai() { return trangThai; }
public void setTrangThai(Qua.TrangThai trangThai) { this.trangThai = trangThai; }

}