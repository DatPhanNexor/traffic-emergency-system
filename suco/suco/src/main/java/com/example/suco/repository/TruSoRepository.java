package com.example.suco.repository;

import com.example.suco.model.TruSo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

public interface TruSoRepository extends JpaRepository<TruSo, Long> {
    Optional<TruSo> findByTenDangNhap(String tenDangNhap);
    
    // Giữ lại cái cũ nếu cần
    List<TruSo> findByGeohashStartingWith(String prefix);

    // THÊM CÁI NÀY: Tìm tất cả trụ sở nằm trong danh sách các ô Geohash
    @Query("SELECT t FROM TruSo t WHERE SUBSTRING(t.geohash, 1, 6) IN :prefixes")
    List<TruSo> findByGeohashIn(List<String> prefixes);
    boolean existsByTenDangNhap(String tenDangNhap);
}