package com.example.suco.service.suco.baocao.truso;

import com.example.suco.dto.SuCoMapDto;
import com.example.suco.model.BaoCaoSuCo;
import com.example.suco.model.TruSo;
import com.example.suco.repository.BaoCaoSuCoRepository;
import com.example.suco.service.suco.baocao.system.mapper.SuCoMapper;
import com.example.suco.service.suco.baocao.system.realtime.BaoCaoRealtimeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TrangThaiService {

    @Autowired
    private BaoCaoSuCoRepository reportRepository;

    @Autowired
    private SuCoMapper suCoMapper;

    @Autowired
    private BaoCaoRealtimeService realtimeService;

    private static final Logger log =
        LoggerFactory.getLogger(TrangThaiService.class);

    @Transactional
    public Map<String, Object> updateSuCoStatus(
            Long id,
            String status,
            TruSo current
    ) {
        log.info("=== UPDATE STATUS START ===");
log.info("Report ID: {}", id);
log.info("Requested status: {}", status);
log.info("TruSo ID: {}", current != null ? current.getId() : null);
        BaoCaoSuCo suCo = reportRepository.findById(id)
        
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sự cố"
                ));
                log.info("=== DB STATE ===");
log.info("CurrentStatus: {}", suCo.getTrangThaiXuLy());
log.info("CurrentTiepNhanId: {}", suCo.getIdTruSoTiepNhan());
log.info("Reporter: {}", 
        suCo.getReporter() != null ? suCo.getReporter().getUid() : null);
        String currentStatus = suCo.getTrangThaiXuLy();
        Long currentTiepNhanId = suCo.getIdTruSoTiepNhan();

        if (status == null || status.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status không được để trống"
            );
        }
        
        List<String> valid = List.of(
                "CHO_XU_LY",
                "DANG_XU_LY",
                "HOAN_THANH",
                "HUY_BO"
        );

        if (!valid.contains(status)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trạng thái không hợp lệ"
            );
        }
        if ("HOAN_THANH".equals(currentStatus)
                || "HUY_BO".equals(currentStatus)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sự cố đã kết thúc, không thể thay đổi trạng thái!"
            );
        }

        if (status.equals(currentStatus)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trạng thái đã là giá trị hiện tại"
            );
        }

        if ("CHO_XU_LY".equals(status)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không được quay lại trạng thái CHỜ XỬ LÝ!"
            );
        }

        if ("DANG_XU_LY".equals(status)) {

            if (!"CHO_XU_LY".equals(currentStatus)) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Chỉ được tiếp nhận từ trạng thái CHỜ XỬ LÝ!"
                );
            }

            suCo.setIdTruSoTiepNhan(current.getId());
            
        }

        else if ("HOAN_THANH".equals(status)) {

            if (!"DANG_XU_LY".equals(currentStatus)) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Phải đang xử lý mới được hoàn thành."
                );
            }

            if (!current.getId().equals(currentTiepNhanId)) {

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Bạn không có quyền hoàn thành sự cố này!"
                );
            }
        }

        else if ("HUY_BO".equals(status)) {

            if (!"CHO_XU_LY".equals(currentStatus)) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Chỉ được hủy khi đang CHỜ XỬ LÝ!"
                );
            }
        }

        suCo.setTrangThaiXuLy(status);

        BaoCaoSuCo saved =
                reportRepository.save(suCo);

        SuCoMapDto dto =
                suCoMapper.convertToDto(saved);

        if (saved.getIdTruSoTiepNhan() != null) {

            realtimeService.broadcastTruSo(
                    saved.getIdTruSoTiepNhan(),
                    dto
            );
        }

        realtimeService.broadcastReport(dto);

        if (saved.getReporter() != null) {

            realtimeService.refreshUserHistory(
                    saved.getReporter().getUid()
            );
        }

        return Map.of(
                "message",
                "Cập nhật trạng thái thành công",

                "newStatus",
                status
        );
    }
    @Transactional
    public void updateProcessStatus(
            Long reportId,
            String status,
            Long idTruSoThucTe
    ) {

        BaoCaoSuCo report = reportRepository.findById(reportId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy báo cáo"
                        ));

        report.setTrangThaiXuLy(status);

        if ("DANG_XU_LY".equals(status)) {

            report.setIdTruSoTiepNhan(
                    idTruSoThucTe
            );
        }

        BaoCaoSuCo saved =
                reportRepository.save(report);

        realtimeService.broadcastReport(
                suCoMapper.convertToDto(saved)
        );

        if (saved.getIdTruSoTiepNhan() != null) {

            realtimeService.broadcastTruSo(
                    saved.getIdTruSoTiepNhan(),
                    suCoMapper.convertToDto(saved)
            );
        }
    }
}