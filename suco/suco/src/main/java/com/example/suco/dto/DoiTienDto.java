package com.example.suco.dto;

public class DoiTienDto {
    private int soDiemDoi;
    private String loaiDoi;

    public DoiTienDto() {}

    public int getSoDiemDoi() { return soDiemDoi; }
    public void setSoDiemDoi(int soDiemDoi) { this.soDiemDoi = soDiemDoi; }

    public String getLoaiDoi() { return loaiDoi; }
    public void setLoaiDoi(String loaiDoi) { this.loaiDoi = loaiDoi; }
}