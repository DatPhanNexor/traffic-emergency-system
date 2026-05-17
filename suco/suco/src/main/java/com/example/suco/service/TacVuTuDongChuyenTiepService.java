    package com.example.suco.service;

    import com.example.suco.service.DieuPhoiSOSService.ThongTinDieuPhoi;

    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.scheduling.annotation.Scheduled;
    import org.springframework.stereotype.Component;

    import java.util.List;

    /**
     * Tác vụ chạy ngầm để tự động chuyển tiếp SOS khi hết thời gian chờ.
     * Kiểm tra mỗi giây xem có SOS nào quá 60 giây chưa được tiếp nhận.
     */
    @Component
    public class TacVuTuDongChuyenTiepService {

        private static final Logger nhatKy = LoggerFactory.getLogger(TacVuTuDongChuyenTiepService.class);
        
        // Thời gian chờ tối đa trước khi chuyển tiếp (giây)
        private static final int THOI_GIAN_CHO_TOI_DA = 60;

        @Autowired
        private DieuPhoiSOSService dieuPhoiService;

        /**
         * Tác vụ chạy mỗi giây để kiểm tra các SOS quá hạn tiếp nhận.
         * Nếu trụ sở hiện tại không tiếp nhận trong 60 giây, tự động chuyển sang trụ sở tiếp theo.
         */
        @Scheduled(fixedRate = 1000) // Chạy mỗi 1 giây
        public void kiemTraVaChuyenTiepQuaHan() {
            try {
                // Lấy danh sách điều phối đã quá thời gian chờ
                List<ThongTinDieuPhoi> danhSachQuaHan = dieuPhoiService.layDanhSachQuaHan(THOI_GIAN_CHO_TOI_DA);

                if (danhSachQuaHan.isEmpty()) {
                    return; // Không có gì cần xử lý
                }

                nhatKy.info("[TÁC VỤ TỰ ĐỘNG] Phát hiện {} SOS quá hạn tiếp nhận", danhSachQuaHan.size());

                for (ThongTinDieuPhoi thongTinDieuPhoi : danhSachQuaHan) {
                    Long idSos = thongTinDieuPhoi.getIdSos();
                    Long idTruSoHienTai = thongTinDieuPhoi.layIdTruSoHienTai();
                    int viTriHienTai = thongTinDieuPhoi.getChiMucTruSoHienTai() + 1;
                    int tongSo = thongTinDieuPhoi.getTongSoTruSo();

                    nhatKy.info("[CHUYỂN TIẾP TỰ ĐỘNG] SOS #{} - Trụ sở {} ({}/{}) không tiếp nhận sau {}s",
                        idSos, idTruSoHienTai, viTriHienTai, tongSo, THOI_GIAN_CHO_TOI_DA);

                    // Thực hiện chuyển tiếp
                    boolean thanhCong = dieuPhoiService.chuyenTiepSangTruSoTiepTheo(thongTinDieuPhoi);
                    
                    if (!thanhCong) {
                        nhatKy.warn("[CẢNH BÁO] SOS #{} đã hết trụ sở có thể tiếp nhận!", idSos);
                    }
                }
            } catch (Exception e) {
                nhatKy.error("[LỖI] Lỗi khi kiểm tra chuyển tiếp tự động: {}", e.getMessage(), e);
            }
        }
    }
