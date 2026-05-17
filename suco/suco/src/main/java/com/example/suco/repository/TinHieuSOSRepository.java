package com.example.suco.repository;

import com.example.suco.model.TinHieuSOS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TinHieuSOSRepository extends JpaRepository<TinHieuSOS, Long> {
    
List<TinHieuSOS> findByUserUid(String uid);

@Query("""
    SELECT s FROM TinHieuSOS s 
    LEFT JOIN FETCH s.user u 
    WHERE s.trangThai NOT IN ('HOAN_THANH', 'HUY_BO') 
    AND (s.idTruSoDeXuat = :idTruSo OR s.idTruSoTiepNhan = :idTruSo)
    ORDER BY u.totalPoints DESC, s.createdAt ASC
""")
List<TinHieuSOS> findActiveByTruSo(@Param("idTruSo") Long idTruSo);

    List<TinHieuSOS> findByIdTruSoTiepNhanAndTrangThaiIn(Long idTruSo, List<String> trangThais);
    
    List<TinHieuSOS> findByIdTruSoTiepNhanAndTrangThai(Long idTruSo, String trangThai);

    // Lấy lịch sử hoàn thành
    @Query("SELECT s FROM TinHieuSOS s WHERE s.trangThai = 'HOAN_THANH' AND s.idTruSoTiepNhan = :idTruSo")
    List<TinHieuSOS> findHistoryByTruSo(@Param("idTruSo") Long idTruSo);
}