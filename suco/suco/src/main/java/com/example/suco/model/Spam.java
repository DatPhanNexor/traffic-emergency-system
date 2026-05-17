package com.example.suco.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "spam")
public class Spam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Lưu UID của người báo cáo để tra cứu (thay vì Join trực tiếp để tránh ràng buộc phức tạp khi xóa user)
    @Column(name = "user_id")
    private String userId;

    @Column(name = "loai_id")
    private Long loaiId;

    private Double kinhDo;
    private Double viDo;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String hinhAnhUrl;

    private String moTa;

    @Column(name = "dia_chi", columnDefinition = "TEXT")
    private String diaChi;

    private LocalDateTime thoiGianSpam = LocalDateTime.now();

    public Spam() {}

    // Constructor nhanh để copy dữ liệu từ BaoCaoSuCo
    public Spam(BaoCaoSuCo report) {
        if (report.getReporter() != null) {
            this.userId = report.getReporter().getUid();
        }
        if (report.getLoaiSuCo() != null) {
            this.loaiId = report.getLoaiSuCo().getId();
        }
        this.kinhDo = report.getKinhDo();
        this.viDo = report.getViDo();
        this.hinhAnhUrl = report.getHinhAnhUrl();
        this.moTa = report.getMoTa();
        this.diaChi = report.getDiaChi();
        this.thoiGianSpam = LocalDateTime.now();
    }

    // ===== Getter & Setter =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Long getLoaiId() { return loaiId; }
    public void setLoaiId(Long loaiId) { this.loaiId = loaiId; }

    public Double getKinhDo() { return kinhDo; }
    public void setKinhDo(Double kinhDo) { this.kinhDo = kinhDo; }

    public Double getViDo() { return viDo; }
    public void setViDo(Double viDo) { this.viDo = viDo; }

    public String getHinhAnhUrl() { return hinhAnhUrl; }
    public void setHinhAnhUrl(String hinhAnhUrl) { this.hinhAnhUrl = hinhAnhUrl; }

    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }

    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }

    public LocalDateTime getThoiGianSpam() { return thoiGianSpam; }
    public void setThoiGianSpam(LocalDateTime thoiGianSpam) { this.thoiGianSpam = thoiGianSpam; }
}