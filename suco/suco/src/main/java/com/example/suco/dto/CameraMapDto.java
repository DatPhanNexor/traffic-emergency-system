package com.example.suco.dto;

public class CameraMapDto {
    private Long id;
    private String tenCamera;
    private Double kinhDo;
    private Double viDo;
    private String anhCamera; // THÊM MỚI
    private String videoUrl;  // THÊM MỚI
    private String moTa;
    private double distance;


    public CameraMapDto() {
    }

    public CameraMapDto(Long id, String tenCamera, Double kinhDo, Double viDo, String anhCamera, String videoUrl, String moTa, double distance) {
        this.id = id;
        this.tenCamera = tenCamera;
        this.kinhDo = kinhDo;
        this.viDo = viDo;
        this.anhCamera = anhCamera;
        this.videoUrl = videoUrl;
        this.moTa = moTa;
        this.distance = distance;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenCamera() {
        return tenCamera;
    }

    public void setTenCamera(String tenCamera) {
        this.tenCamera = tenCamera;
    }

    public Double getKinhDo() {
        return kinhDo;
    }

    public void setKinhDo(Double kinhDo) {
        this.kinhDo = kinhDo;
    }

    public Double getViDo() {
        return viDo;
    }

    public void setViDo(Double viDo) {
        this.viDo = viDo;
    }
    public String getAnhCamera() {
        return anhCamera;
    }
    public void setAnhCamera(String anhCamera) {
        this.anhCamera = anhCamera;
    }
    public String getVideoUrl() {
        return videoUrl;
    }
    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
    public String getMoTa() {
        return moTa;
    }
    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }
    public double getDistance() {
        return distance;
    }
    public void setDistance(double distance) {
        this.distance = distance;
    }

    
}