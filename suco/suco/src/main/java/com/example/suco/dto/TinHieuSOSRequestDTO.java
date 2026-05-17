package com.example.suco.dto;

public class TinHieuSOSRequestDTO {
    private double viDo;
    private double kinhDo;
    private String ghiAmBase64;
    private String hinhAnhBase64;
    private String ghiChu;
    private String thoiGianTao;
    private String diaChi; // Thêm trường

    // Constructor mặc định (Bắt buộc để Jackson map JSON)
    public TinHieuSOSRequestDTO() {}

    public double getViDo() { return viDo; }
    public void setViDo(double viDo) { this.viDo = viDo; }

    public double getKinhDo() { return kinhDo; }
    public void setKinhDo(double kinhDo) { this.kinhDo = kinhDo; }

    public String getGhiAmBase64() { return ghiAmBase64; }
    public void setGhiAmBase64(String ghiAmBase64) { this.ghiAmBase64 = ghiAmBase64; }

    public String getHinhAnhBase64() { return hinhAnhBase64; }
    public void setHinhAnhBase64(String hinhAnhBase64) { this.hinhAnhBase64 = hinhAnhBase64; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public String getThoiGianTao() { return thoiGianTao; }
    public void setThoiGianTao(String thoiGianTao) { this.thoiGianTao = thoiGianTao; }

public String getDiaChi() { return diaChi; } // Thêm Getter
public void setDiaChi(String diaChi) { this.diaChi = diaChi; } // Thêm Setter
}