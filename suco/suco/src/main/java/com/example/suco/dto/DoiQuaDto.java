package com.example.suco.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DoiQuaDto {
    private Long quaId;
    private String tenQua;
    private Integer soLuong; // THÊM TRƯỜNG NÀY
    private String loai;    // Nên thêm loai để Android biết là VOUCHER hay SAN_PHAM
    private Integer giaTriGiamPercent;
    private java.math.BigDecimal giaTriToiDa;
    private LocalDateTime ngayKetThuc;

  public DoiQuaDto() {}



    public Long getQuaId() { return quaId; }
    public void setQuaId(Long quaId) { this.quaId = quaId; }

    public String getTenQua() { return tenQua; }
    public void setTenQua(String tenQua) { this.tenQua = tenQua; }

    public Integer getSoLuong() { return soLuong; }
    public void setSoLuong(Integer soLuong) { this.soLuong = soLuong; }

    public String getLoai() { return loai; }
    public void setLoai(String loai) { this.loai = loai; }

    public Integer getGiaTriGiamPercent() { return giaTriGiamPercent; }
    public void setGiaTriGiamPercent(Integer giaTriGiamPercent) { this.giaTriGiamPercent = giaTriGiamPercent; }

    public java.math.BigDecimal getGiaTriToiDa() { return giaTriToiDa; }
    public void setGiaTriToiDa(java.math.BigDecimal giaTriToiDa) { this.giaTriToiDa = giaTriToiDa; }

    public LocalDateTime getNgayKetThuc() { return ngayKetThuc; }
    public void setNgayKetThuc(LocalDateTime ngayKetThuc) { this.ngayKetThuc = ngayKetThuc; }
  
}