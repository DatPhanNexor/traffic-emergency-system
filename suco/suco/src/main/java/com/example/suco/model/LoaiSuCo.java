package com.example.suco.model;

import jakarta.persistence.*;

@Entity
public class LoaiSuCo {
    @Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

    private String ten;
    private String iconUrl;   // icon của loại sự cố

    // getters và setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTen() { return ten; }
    public void setTen(String ten) { this.ten = ten; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
}
