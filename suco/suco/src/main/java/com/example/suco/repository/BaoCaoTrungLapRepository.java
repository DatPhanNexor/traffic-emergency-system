package com.example.suco.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.suco.model.BaoCaoTrungLap;

@Repository
public interface BaoCaoTrungLapRepository extends JpaRepository<BaoCaoTrungLap, Long> {

    // Check user đã từng góp tin cậy chưa
    boolean existsByBaoCao_IdAndUserId(Long baoCaoId, String userId);
    @Query("SELECT COUNT(b) FROM BaoCaoTrungLap b WHERE b.baoCao.id = :baoCaoId")
int countByBaoCaoId(@Param("baoCaoId") Long baoCaoId);

}