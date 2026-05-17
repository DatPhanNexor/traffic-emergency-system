    package com.example.suco.model;

    import jakarta.persistence.*;
    import java.time.LocalDateTime;

    @Entity
    @Table(name = "tin_hieu_sos")
    public class TinHieuSOS {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "user_id")
        private String userId;

        @ManyToOne(fetch = FetchType.EAGER) // Thêm fetch type này
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore

    private User user;

        @Column(name = "vi_do")
        private Double viDo;

        @Column(name = "kinh_do")
        private Double kinhDo;

        @Column(name = "ghi_am")
        private String ghiAm;

        @Column(name = "hinh_anh")
        private String hinhAnh;

        @Column(name = "ghi_chu", columnDefinition = "TEXT")
        private String ghiChu;
    @Column(name = "id_tru_so_de_xuat")
    private Long idTruSoDeXuat;

        // --- THÊM 2 CỘT NÀY ---
        @Column(name = "id_tru_so_tiep_nhan")
        private Long idTruSoTiepNhan;

        @Column(name = "trang_thai")
        private String trangThai = "CHO_XU_LY"; // CHO_XU_LY, DANG_XU_LY,HOAN_THANH
        // ----------------------

        @Column(name = "created_at")
        private LocalDateTime createdAt;
    // Thêm trường này vào sau trangThai
    @Column(name = "dia_chi", columnDefinition = "TEXT")
    private String diaChi;
    // Trong file TinHieuSOS.java
    @Transient
    private boolean isVip; // Trường này không lưu DB, chỉ dùng để gửi ra Web
    
@OneToOne(fetch = FetchType.EAGER) // Một SOS có 1 Hóa đơn, load kèm luôn khi lấy SOS
@JoinColumn(name = "hoa_don_id") // Tạo cột hoa_don_id trong bảng tin_hieu_sos
private HoaDon hoaDon;

// Đừng quên thêm Getter/Setter cho hoaDon

public HoaDon getHoaDon() { return hoaDon; }
public void setHoaDon(HoaDon hoaDon) { this.hoaDon = hoaDon; }

    public boolean getIsVip() { return isVip; }
    public void setIsVip(boolean isVip) { this.isVip = isVip; }
    // Thêm Getter & Setter
    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
        public TinHieuSOS() {}

        @PrePersist
        protected void onCreate() {
            this.createdAt = LocalDateTime.now();
        }

        // Getter & Setter (Nhớ thêm getter/setter cho idTruSoTiepNhan và trangThai)
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
            public String getUserId() { return userId; }
            public void setUserId(String userId) { this.userId = userId; }
            public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
        public Double getViDo() { return viDo; }
        public void setViDo(Double viDo) { this.viDo = viDo; }
        public Double getKinhDo() { return kinhDo; }
        public void setKinhDo(Double kinhDo) { this.kinhDo = kinhDo; }
        public String getGhiAm() { return ghiAm; }
        public void setGhiAm(String ghiAm) { this.ghiAm = ghiAm; }
        public String getHinhAnh() { return hinhAnh; }
        public void setHinhAnh(String hinhAnh) { this.hinhAnh = hinhAnh; }
        public String getGhiChu() { return ghiChu; }
        public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
        public Long getIdTruSoDeXuat() { return idTruSoDeXuat; }
        public void setIdTruSoDeXuat(Long idTruSoDeXuat) { this.idTruSoDeXuat = idTruSoDeXuat; }
        public Long getIdTruSoTiepNhan() { return idTruSoTiepNhan; }
        public void setIdTruSoTiepNhan(Long idTruSoTiepNhan) { this.idTruSoTiepNhan = idTruSoTiepNhan; }
        public String getTrangThai() { return trangThai; }
        public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }