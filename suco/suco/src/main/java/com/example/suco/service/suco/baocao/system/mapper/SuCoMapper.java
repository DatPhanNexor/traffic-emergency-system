package com.example.suco.service.suco.baocao.system.mapper;

import com.example.suco.dto.SuCoMapDto;
import com.example.suco.dto.TruSoMapDto;
import com.example.suco.model.BaoCaoSuCo;
import com.example.suco.model.TruSo;
import com.example.suco.repository.TruSoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SuCoMapper {

    @Autowired
    private TruSoRepository truSoRepository;

    public SuCoMapDto convertToDto(BaoCaoSuCo b) {

        String tenLoai =
                (b.getLoaiSuCo() != null)
                        ? b.getLoaiSuCo().getTen()
                        : "Không xác định";

        String iconUrl =
                (b.getLoaiSuCo() != null)
                        ? b.getLoaiSuCo().getIconUrl()
                        : "";

        String tenNguoiBao =
                (b.getReporter() != null)
                        ? b.getReporter().getName()
                        : "Người dân báo";

        SuCoMapDto dto = new SuCoMapDto(
                b.getId(),
                b.getViDo(),
                b.getKinhDo(),
                b.getMoTa(),
                tenLoai,
                b.getTrangThaiDuyet(),
                b.getTrangThaiXuLy(),
                iconUrl,
                b.getMucDoNghiemTrong(),
                b.getHinhAnhUrl(),
                b.getDoTinCay(),
                null,
                null,
                null,
                null,
                b.getDiaChi(),
                tenNguoiBao
        );

        if (b.getReporter() != null) {
            dto.setReporterUid(b.getReporter().getUid());
        }

        if (b.getIdTruSoDeXuat() != null) {

            TruSo ts = truSoRepository
                    .findById(b.getIdTruSoDeXuat())
                    .orElse(null);

            if (ts != null) {

                dto.setTruSoDeXuat(
                        new TruSoMapDto(
                                ts.getId(),
                                ts.getTenTruSo(),
                                ts.getKinhDo(),
                                ts.getViDo()
                        )
                );
            }
        }

        if (b.getIdTruSoTiepNhan() != null) {

            TruSo ts = truSoRepository
                    .findById(b.getIdTruSoTiepNhan())
                    .orElse(null);

            if (ts != null) {

                dto.setTruSoTiepNhan(
                        new TruSoMapDto(
                                ts.getId(),
                                ts.getTenTruSo(),
                                ts.getKinhDo(),
                                ts.getViDo()
                        )
                );
            }
        }

        return dto;
    }
}