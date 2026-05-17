    package com.example.suco.repository;

    import com.example.suco.dto.SuCoMapDto;
    import com.example.suco.model.BaoCaoSuCo;
    import com.example.suco.model.TinHieuSOS;

    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;
    import java.util.List;

    public interface BaoCaoSuCoRepository extends JpaRepository<BaoCaoSuCo, Long> {

        List<BaoCaoSuCo> findByTrangThaiXuLyNot(String status);
        List<BaoCaoSuCo> findByTrangThaiDuyetIn(List<String> statuses);
        List<BaoCaoSuCo> findByTrangThaiXuLyNotIn(List<String> statuses);
        // Chỉ cần một constructor khớp với dữ liệu thực tế
    @Query("""
            SELECT new com.example.suco.dto.SuCoMapDto(
                b.id, b.viDo, b.kinhDo, b.moTa, b.loaiSuCo.ten,
                b.trangThaiDuyet, b.trangThaiXuLy, b.loaiSuCo.iconUrl, 
                b.mucDoNghiemTrong, b.hinhAnhUrl,
                b.doTinCay, null, null, null, null, b.diaChi, b.reporter.name, b.reporter.uid
            )
            FROM BaoCaoSuCo b
            WHERE b.trangThaiDuyet IN ('AI_APPROVED', 'VERIFIED')
            AND b.trangThaiXuLy NOT IN ('HOAN_THANH', 'HUY_BO') 
            """)
        List<SuCoMapDto> findAllForMap();

        // 2. Lấy dữ liệu cho bản đồ của từng Trụ sở
    @Query("""
            SELECT new com.example.suco.dto.SuCoMapDto(
                b.id, b.viDo, b.kinhDo, b.moTa, b.loaiSuCo.ten,
                b.trangThaiDuyet, b.trangThaiXuLy, b.loaiSuCo.iconUrl,
                b.mucDoNghiemTrong, b.hinhAnhUrl,
                b.doTinCay, null, null, null, null, b.diaChi, b.reporter.name, b.reporter.uid
            )
            FROM BaoCaoSuCo b
            WHERE (
                (b.trangThaiXuLy = 'CHO_XU_LY' AND b.idTruSoDeXuat = :idTruSo) 
                OR 
                (b.trangThaiXuLy = 'DANG_XU_LY' AND b.idTruSoTiepNhan = :idTruSo)
            )
            AND b.trangThaiDuyet != 'REJECTED'
            AND b.trangThaiXuLy != 'HUY_BO' 
        """)
        List<SuCoMapDto> findActiveByTruSo(@Param("idTruSo") Long idTruSo);


        @Query("SELECT b FROM BaoCaoSuCo b WHERE b.trangThaiXuLy = 'HOAN_THANH' AND b.idTruSoTiepNhan = :idTruSo")
        List<BaoCaoSuCo> findHistoryByTruSo(@Param("idTruSo") Long idTruSo);

        List<BaoCaoSuCo> findByReporterUid(String uid);

    @Query("""
        SELECT b FROM BaoCaoSuCo b 
        WHERE b.trangThaiDuyet IN ('AI_APPROVED', 'PENDING') 
        AND b.trangThaiXuLy = 'CHO_XU_LY'
        ORDER BY b.thoiGianTao DESC
    """)
    List<BaoCaoSuCo> findPendingReportsForAdmin();
    }