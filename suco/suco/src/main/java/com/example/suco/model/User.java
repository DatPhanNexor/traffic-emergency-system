package com.example.suco.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "users")
public class User {

    @Id
    private String uid;      // Firebase UID

    private String email;
    

@Column(name = "password")
private String password;
    private String name;
    private String provider;

    // Bổ sung các trường mới
    @Column(name = "total_points")
private int totalPoints;

@Column(name = "spam_count")
private int spamCount; // Số lần bị Admin đánh dấu báo cáo sai

@Column(name = "role")
private String role; // USER / ADMIN

    @Transient 
    private String tenGoi;

    public User() {
    }

    // ===== Getter & Setter =====
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

    public int getSpamCount() { return spamCount; }
    public void setSpamCount(int spamCount) { this.spamCount = spamCount; }

    public String getTenGoi() { return tenGoi; }
    public void setTenGoi(String tenGoi) { this.tenGoi = tenGoi; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}