package com.example.suco.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cameras")
public class Camera {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tenCamera;
    private Double kinhDo;
    private Double viDo;
    private String moTa; // Miêu tả chính xác vị trí camera
    private String anhCamera; // Đường dẫn ảnh chụp camera
    private String videoUrl; // Đường dẫn video demo (có thể là video giả)
    @Column(length = 8)
private String geohash
;
    public Camera() {   // Constructor mặc định
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



    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
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
    // Getter và Setter cho geohash
public String getGeohash() { return geohash; }
public void setGeohash(String geohash) { this.geohash = geohash; }
}