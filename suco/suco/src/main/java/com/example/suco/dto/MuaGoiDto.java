package com.example.suco.dto;

import java.time.LocalDateTime;

public class MuaGoiDto {
    private Long id;
    private String userId;
    private Long goiId;
    private String tenGoi; 
    private LocalDateTime ngayMua;
    private LocalDateTime ngayHetHan; // Cột này sẽ lấy giá trị từ Entity MuaGoi
    private String trangThai;

    // Constructor cập nhật để nhận ngayHetHan
    public MuaGoiDto(Long id, String userId, Long goiId, String tenGoi, 
                      LocalDateTime ngayMua, LocalDateTime ngayHetHan, String trangThai) {
        this.id = id;
        this.userId = userId;
        this.goiId = goiId;
        this.tenGoi = tenGoi;
        this.ngayMua = ngayMua;
        this.ngayHetHan = ngayHetHan;
        this.trangThai = trangThai;
    }

    // Getters ...
    public LocalDateTime getNgayHetHan() { return ngayHetHan; }
    public void setNgayHetHan(LocalDateTime ngayHetHan) { this.ngayHetHan = ngayHetHan; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Long getGoiId() { return goiId; }
    public void setGoiId(Long goiId) { this.goiId = goiId; }
    public String getTenGoi() { return tenGoi; }
    public void setTenGoi(String tenGoi) { this.tenGoi = tenGoi; }
    public LocalDateTime getNgayMua() { return ngayMua; }
    public void setNgayMua(LocalDateTime ngayMua) { this.ngayMua = ngayMua; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    
}