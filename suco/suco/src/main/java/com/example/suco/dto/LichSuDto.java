package com.example.suco.dto;

import com.example.suco.model.HoaDon;

public class LichSuDto {
    private Long id;
    private String loai;
    private String tieuDe;
    private String moTa;
    private String trangThaiXuLy;
    private String trangThaiDuyet;
    private String hinhAnhUrl;
    private Double viDo;
    private Double kinhDo;
    private String ghiAmUrl;
    private String tenTruSoTiepNhan;
    private String thoiGian;
    
    // THÊM TRƯỜNG NÀY ĐỂ MOBILE NHẬN ĐỊA CHỈ
    private String diaChi; 
    private HoaDon hoaDon;

    public LichSuDto() {}

    // Cập nhật Constructor (Sửa lại constructor để nhận thêm diaChi)
    public LichSuDto(Long id, String loai, String tieuDe, String moTa, String trangThaiXuLy, 
                     String trangThaiDuyet, String hinhAnhUrl, Double viDo, Double kinhDo, 
                     String ghiAmUrl, String tenTruSoTiepNhan, String thoiGian, String diaChi, HoaDon hoaDon) {
        this.id = id;
        this.loai = loai;
        this.tieuDe = tieuDe;
        this.moTa = moTa;
        this.trangThaiXuLy = trangThaiXuLy;
        this.trangThaiDuyet = trangThaiDuyet;
        this.hinhAnhUrl = hinhAnhUrl;
        this.viDo = viDo;
        this.kinhDo = kinhDo;
        this.ghiAmUrl = ghiAmUrl;
        this.tenTruSoTiepNhan = tenTruSoTiepNhan;
        this.thoiGian = thoiGian;
        this.diaChi = diaChi;
        this.hoaDon = hoaDon;
    }

    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }

    public HoaDon getHoaDon() { return hoaDon; }
    public void setHoaDon(HoaDon hoaDon) { this.hoaDon = hoaDon; }
    
    // Getters và Setters...
    public Long getId() { return id; }
    public String getLoai() { return loai; }
    public String getTrangThaiXuLy() { return trangThaiXuLy; }
    public String getTrangThaiDuyet() { return trangThaiDuyet; }
    public String getHinhAnhUrl() { return hinhAnhUrl; }
    public Double getViDo() { return viDo; }
    public Double getKinhDo() { return kinhDo; }
    public String getGhiAmUrl() { return ghiAmUrl; }
    public String getTenTruSoTiepNhan() { return tenTruSoTiepNhan; }
    public String getThoiGian() { return thoiGian; }
    public String getTieuDe() { return tieuDe; }
    public String getMoTa() { return moTa; }
    public void setId(Long id) { this.id = id; }
    public void setLoai(String loai) { this.loai = loai; }
    public void setTrangThaiXuLy(String trangThaiXuLy) { this.trangThaiXuLy = trangThaiXuLy; }
    public void setTrangThaiDuyet(String trangThaiDuyet) { this.trangThaiDuyet = trangThaiDuyet; }
    public void setHinhAnhUrl(String hinhAnhUrl) { this.hinhAnhUrl = hinhAnhUrl; }
    public void setViDo(Double viDo) { this.viDo = viDo; }
    public void setKinhDo(Double kinhDo) { this.kinhDo = kinhDo; }
    public void setGhiAmUrl(String ghiAmUrl) { this.ghiAmUrl = ghiAmUrl; }
    public void setTenTruSoTiepNhan(String tenTruSoTiepNhan) { this.tenTruSoTiepNhan = tenTruSoTiepNhan; }
    public void setThoiGian(String thoiGian) { this.thoiGian = thoiGian; }
    public void setTieuDe(String tieuDe) { this.tieuDe = tieuDe; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    
}