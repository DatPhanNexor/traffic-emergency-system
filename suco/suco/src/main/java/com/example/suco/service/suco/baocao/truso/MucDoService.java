package com.example.suco.service.suco.baocao.truso;

import com.example.suco.dto.SuCoMapDto;
import com.example.suco.model.BaoCaoSuCo;
import com.example.suco.model.TruSo;
import com.example.suco.repository.BaoCaoSuCoRepository;
import com.example.suco.service.suco.baocao.system.mapper.SuCoMapper;
import com.example.suco.service.suco.baocao.system.realtime.BaoCaoRealtimeService;
import com.example.suco.service.suco.baocao.system.validation.QuyenHanService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class MucDoService {

    private static final Logger log =
            LoggerFactory.getLogger(MucDoService.class);

    @Autowired
    private BaoCaoSuCoRepository reportRepository;

    @Autowired
    private QuyenHanService quyenHanService;

    @Autowired
    private BaoCaoRealtimeService realtimeService;

    @Autowired
    private SuCoMapper suCoMapper;

    @Transactional
    public Map<String, Object> capNhatMucDo(
            Long id,
            String mucDo,
            TruSo current
    ) {

        BaoCaoSuCo suCo = reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sự cố"
                ));

        log.info(
                "\nTrụ sở {} cập nhật mức độ {} sự cố: {}"
                        + "\nTrạng thái xử lý hiện tại: {}"
                        + "\nMức độ hiện tại: {}"
                        + "\nTrụ sở tiếp nhận hiện tại: {}",
                current.getTenTruSo(),
                mucDo,
                id,
                suCo.getTrangThaiXuLy(),
                suCo.getMucDoNghiemTrong(),
                suCo.getIdTruSoTiepNhan()
        );

        String status = suCo.getTrangThaiXuLy();

        if ("HOAN_THANH".equals(status)
                || "HUY_BO".equals(status)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sự cố đã kết thúc, không thể thay đổi mức độ nghiêm trọng!"
            );
        }

        if (!"DANG_XU_LY".equals(status)) {

            log.error(
                    "\nSự cố ID {} đang ở trạng thái '{}', không cho phép cập nhật mức độ!",
                    id,
                    status
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ được cập nhật mức độ khi sự cố đang xử lý!"
            );
        }

        Long idTruSo = suCo.getIdTruSoTiepNhan();

        if (idTruSo == null) {

            log.error(
                    "\nSự cố ID {} chưa có trụ sở tiếp nhận, không thể cập nhật mức độ!",
                    id
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sự cố chưa được tiếp nhận!"
            );
        }

        if (!current.getId().equals(suCo.getIdTruSoTiepNhan())) {

            log.error(
                    "\nSự cố ID {} không thuộc trụ sở {}!",
                    id,
                    current.getTenTruSo()
            );

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền chỉnh sửa mức độ của sự cố này!"
            );
        }

        if (mucDo == null || mucDo.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mức độ không được để trống!"
            );
        }

        List<String> validLevels =
                List.of("LOW", "MEDIUM", "HIGH");

        if (!validLevels.contains(mucDo.toUpperCase())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mức độ không hợp lệ! (LOW, MEDIUM, HIGH)"
            );
        }

        mucDo = mucDo.toUpperCase();

        suCo.setMucDoNghiemTrong(mucDo);

        BaoCaoSuCo saved =
                reportRepository.save(suCo);

        SuCoMapDto dto =
                suCoMapper.convertToDto(saved);

        log.info(
                "\nCập nhật mức độ thành công. Mức độ mới: {}"
                        + "\nID sự cố: {}"
                        + "\nTrụ sở tiếp nhận: {}"
                        + "\nTrạng thái xử lý: {}",
                suCo.getMucDoNghiemTrong(),
                id,
                suCo.getIdTruSoTiepNhan(),
                suCo.getTrangThaiXuLy()
        );

        if (saved.getIdTruSoTiepNhan() != null) {

            realtimeService.broadcastTruSo(
                    saved.getIdTruSoTiepNhan(),
                    dto
            );
        }

        realtimeService.broadcastReport(dto);

        return Map.of(
                "message",
                "Cập nhật mức độ thành công",

                "mucDoMoi",
                mucDo
        );
    }
}