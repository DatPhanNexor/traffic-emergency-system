package com.example.suco.service.suco.baocao.system.validation;

import com.example.suco.model.BaoCaoSuCo;
import com.example.suco.model.BaoCaoTrungLap;
import com.example.suco.repository.BaoCaoSuCoRepository;
import com.example.suco.repository.BaoCaoTrungLapRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrungLapBaoCaoService {

    @Autowired
    private BaoCaoSuCoRepository reportRepository;

    @Autowired
    private BaoCaoTrungLapRepository baoCaoTrungLapRepository;

    public BaoCaoSuCo findDuplicateReport(BaoCaoSuCo report) {

        List<BaoCaoSuCo> activeReports =
                reportRepository.findByTrangThaiXuLyNotIn(
                        List.of("HOAN_THANH", "HUY_BO")
                );

        for (BaoCaoSuCo ex : activeReports) {

            double distanceMeters = calculateDistance(
                    report.getViDo(),
                    report.getKinhDo(),
                    ex.getViDo(),
                    ex.getKinhDo()
            );

            if (distanceMeters <= 20
                    && ex.getLoaiSuCo().getId()
                    .equals(report.getLoaiSuCo().getId())) {

                return ex;
            }
        }

        return null;
    }

    public Double calculateMatchedDistance(
            BaoCaoSuCo report,
            BaoCaoSuCo existingReport
    ) {

        return calculateDistance(
                report.getViDo(),
                report.getKinhDo(),
                existingReport.getViDo(),
                existingReport.getKinhDo()
        );
    }

    public boolean isUserAlreadyContributed(
            Long reportId,
            String uid
    ) {

        return baoCaoTrungLapRepository
                .existsByBaoCao_IdAndUserId(
                        reportId,
                        uid
                );
    }

    public void saveDuplicateContributor(
            BaoCaoSuCo existingReport,
            String uid
    ) {

        baoCaoTrungLapRepository.save(
                new BaoCaoTrungLap(
                        existingReport,
                        uid
                )
        );
    }

    public void recalculateTrust(BaoCaoSuCo report) {

        int trust =
                baoCaoTrungLapRepository.countByBaoCaoId(report.getId()) + 1;

        report.setDoTinCay(trust);
    }

    private double calculateDistance(
            double lat1,
            double lon1,
            double lat2,
            double lon2
    ) {

        double R = 6371e3;

        double dPhi =
                Math.toRadians(lat2 - lat1);

        double dLambda =
                Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLambda / 2)
                        * Math.sin(dLambda / 2);

        return R * 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a)
        );
    }
}