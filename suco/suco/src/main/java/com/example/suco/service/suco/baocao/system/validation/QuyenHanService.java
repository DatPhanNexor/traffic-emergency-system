package com.example.suco.service.suco.baocao.system.validation;

import com.example.suco.model.BaoCaoSuCo;
import com.example.suco.model.TruSo;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuyenHanService {

    public void checkOwner(BaoCaoSuCo report, String currentUid) {

        if (report.getReporter() == null
                || !report.getReporter().getUid().equals(currentUid)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền thao tác báo cáo này."
            );
        }
    }

    public void checkTruSoPermission(
            BaoCaoSuCo suCo,
            TruSo current
    ) {

        if (suCo.getIdTruSoTiepNhan() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sự cố chưa được tiếp nhận!"
            );
        }

        if (!current.getId().equals(suCo.getIdTruSoTiepNhan())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền thao tác sự cố này!"
            );
        }
    }

    public void checkNotFinished(String status) {

        if ("HOAN_THANH".equals(status)
                || "HUY_BO".equals(status)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sự cố đã kết thúc, không thể thay đổi!"
            );
        }
    }
}