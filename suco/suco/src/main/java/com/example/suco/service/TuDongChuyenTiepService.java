package com.example.suco.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Component riêng để xử lý các task định kỳ cho hệ thống SOS.
 */
@Component
public class TuDongChuyenTiepService {

    private static final Logger logger = LoggerFactory.getLogger(TuDongChuyenTiepService.class);

    @Autowired
    private DieuPhoiSOSService dieuPhoiService;

    /**
     * Task tự động chạy mỗi 10 giây để kiểm tra và chuyển tiếp SOS quá hạn.
     */
    @Scheduled(fixedRate = 10000) // Chạy mỗi 10 giây
    public void xuLyChuyenTiepTuDong() {
        try {
            List<DieuPhoiSOSService.ThongTinDieuPhoi> danhSachQuaHan = dieuPhoiService.layDanhSachQuaHan(DieuPhoiSOSService.THOI_GIAN_CHO_TIEP_NHAN);

            if (danhSachQuaHan.isEmpty()) {
                return; // Không có SOS nào quá hạn
            }

            logger.info("[AUTO TRANSFER] Phát hiện {} SOS quá hạn, bắt đầu chuyển tiếp...", danhSachQuaHan.size());

            for (DieuPhoiSOSService.ThongTinDieuPhoi thongTin : danhSachQuaHan) {
                try {
                    boolean thanhCong = dieuPhoiService.chuyenTiepSangTruSoTiepTheo(thongTin);
                    if (!thanhCong) {
                        logger.warn("[AUTO TRANSFER] Không thể chuyển tiếp SOS #{} - hết trụ sở", thongTin.getIdSos());
                    }
                } catch (Exception e) {
                    logger.error("[AUTO TRANSFER] Lỗi khi chuyển tiếp SOS #{}: {}", thongTin.getIdSos(), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("[AUTO TRANSFER] Lỗi trong task tự động: {}", e.getMessage());
        }
    }
}
