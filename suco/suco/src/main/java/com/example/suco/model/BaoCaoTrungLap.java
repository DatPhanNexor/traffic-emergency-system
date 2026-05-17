package com.example.suco.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(
    name = "bao_cao_trung_lap",
    uniqueConstraints = @UniqueConstraint(columnNames = {"bao_cao_id", "user_id"})
)
public class BaoCaoTrungLap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết tới báo cáo chính
    @ManyToOne
    @JoinColumn(name = "bao_cao_id", nullable = false)
    private BaoCaoSuCo baoCao;

    // User đã góp tin cậy
    @Column(name = "user_id", nullable = false)
    private String userId;

    // Thời gian góp
    private LocalDateTime thoiGianTao = LocalDateTime.now();

    // ===== Constructor =====
    public BaoCaoTrungLap() {}

    public BaoCaoTrungLap(BaoCaoSuCo baoCao, String userId) {
        this.baoCao = baoCao;
        this.userId = userId;
    }

    // ===== Getter & Setter =====
    public Long getId() { return id; }

    public BaoCaoSuCo getBaoCao() { return baoCao; }
    public void setBaoCao(BaoCaoSuCo baoCao) { this.baoCao = baoCao; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public LocalDateTime getThoiGianTao() { return thoiGianTao; }
    public void setThoiGianTao(LocalDateTime thoiGianTao) { this.thoiGianTao = thoiGianTao; }
}