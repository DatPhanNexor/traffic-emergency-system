package com.example.suco.repository;

import com.example.suco.model.DoiTien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DoiTienRepository extends JpaRepository<DoiTien, Long> {
    DoiTien findFirstByUserIdAndLoaiDoiAndNgayDoiBetween(
        String userId, String loaiDoi, LocalDateTime start, LocalDateTime end
    );

    @Query("SELECT SUM(d.giaTri) FROM DoiTien d WHERE d.loaiDoi = 'QUYEN_GOP'")
    Long sumAllDonationValues(); // Tính tổng tiền thay vì tổng điểm

    List<DoiTien> findTop10ByLoaiDoiOrderByNgayDoiDesc(String loaiDoi);
List<DoiTien> findByUserId(String userId);
List<DoiTien> findByLoaiDoi(String loaiDoi);
List<DoiTien> findByUserIdAndLoaiDoi(String userId, String loaiDoi);}