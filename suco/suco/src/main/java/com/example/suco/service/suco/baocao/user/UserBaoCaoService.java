package com.example.suco.service.suco.baocao.user;

import com.example.suco.dto.SuCoMapDto;
import com.example.suco.service.AiVerifyResult;
import com.example.suco.service.suco.baocao.system.file.ImageStorageService;
import com.example.suco.service.suco.baocao.system.location.GeocodingService;
import com.example.suco.service.suco.baocao.system.mapper.SuCoMapper;
import com.example.suco.service.suco.baocao.system.realtime.BaoCaoRealtimeService;
import com.example.suco.service.suco.baocao.system.reward.UserRewardService;
import com.example.suco.service.suco.baocao.system.validation.AiVerificationService;
import com.example.suco.service.suco.baocao.system.validation.QuyenHanService;
import com.example.suco.service.suco.baocao.system.validation.TrungLapBaoCaoService;
import com.example.suco.model.BaoCaoSuCo;
import com.example.suco.model.LoaiSuCo;
import com.example.suco.model.User;
import com.example.suco.repository.BaoCaoSuCoRepository;
import com.example.suco.repository.LoaiSuCoRepository;
import com.example.suco.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class UserBaoCaoService {

    private static final Logger log =
            LoggerFactory.getLogger(UserBaoCaoService.class);

    @Autowired
    private BaoCaoSuCoRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoaiSuCoRepository loaiSuCoRepository;

    @Autowired
    private AiVerificationService aiVerificationService;

    @Autowired
    private TrungLapBaoCaoService trungLapBaoCaoService;

    @Autowired
    private UserRewardService userRewardService;

    @Autowired
    private BaoCaoRealtimeService realtimeService;

    @Autowired
    private SuCoMapper suCoMapper;

    @Autowired
    private GeocodingService geocodingService;

    @Autowired
    private ImageStorageService imageStorageService;

    @Autowired
    private QuyenHanService quyenHanService;

    public List<SuCoMapDto> getMyReports(String uid) {

        return reportRepository.findByReporterUid(uid)
                .stream()
                .map(suCoMapper::convertToDto)
                .toList();
    }

    public List<SuCoMapDto> getPendingReportsForAdmin() {

        return reportRepository.findPendingReportsForAdmin()
                .stream()
                .map(suCoMapper::convertToDto)
                .toList();
    }

     private String maskUid(String uid) {

        if (uid == null) {
            return "null";
        }

        return uid.length() > 5
                ? uid.substring(0, 5) + "***"
                : uid + "***";
    }

    @Transactional
    public AiVerifyResult submitReport(
            String uid,
            BaoCaoSuCo report,
            String base64FullData
    ) {

        LoaiSuCo loaiSuCo =
                loaiSuCoRepository.findById(
                                report.getLoaiSuCo().getId()
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Loại sự cố không tồn tại"
                                ));

        report.setLoaiSuCo(loaiSuCo);

        AiVerifyResult ai =
                aiVerificationService.verifyReportImage(
                        base64FullData,
                        loaiSuCo.getTen()
                );

        if (!ai.isValid()) {
            return ai;
        }

        BaoCaoSuCo existingReport =
                trungLapBaoCaoService.findDuplicateReport(report);

        Double matchedDistance = null;

        if (existingReport != null) {

            matchedDistance =
                    trungLapBaoCaoService.calculateMatchedDistance(
                            report,
                            existingReport
                    );
        }

        String currentUserId = uid;

        User currentReporter =
                userRepository.findById(uid).orElse(null);

        if (existingReport != null) {

            System.out.println(
                    "Bắt gặp báo cáo trùng lập với ID: "
                            + existingReport.getId()
            );

            if (existingReport.getReporter()
                    .getUid()
                    .equals(uid)) {

                AiVerifyResult result =
                        new AiVerifyResult(
                                false,
                                100,
                                "Bạn đã báo cáo sự cố này trước đó"
                        );

                result.setDistance(matchedDistance);

                log.info(
                        "\n[TRÙNG - CHÍNH CHỦ]"
                                + "\nUser: {}"
                                + "\nReport ID: {}"
                                + "\nLoại sự cố: {}"
                                + "\nKhoảng cách: {} m"
                                + "\nĐộ tin cậy report hiện tại: {}"
                                + "\nĐiểm hiện tại của user: {}\n",

                        maskUid(uid),
                        existingReport.getId(),
                        existingReport.getLoaiSuCo().getTen(),
                        matchedDistance,
                        existingReport.getDoTinCay(),

                        currentReporter != null
                                ? currentReporter.getTotalPoints()
                                : "N/A"
                );

                return result;
            }

            boolean existed =
                    trungLapBaoCaoService.isUserAlreadyContributed(
                            existingReport.getId(),
                            uid
                    );

            if (existed) {

                AiVerifyResult result =
                        new AiVerifyResult(
                                false,
                                100,
                                "Bạn đã báo cáo sự cố này trước đó"
                        );

                result.setDistance(matchedDistance);

                return result;
            }

            trungLapBaoCaoService.saveDuplicateContributor(
                    existingReport,
                    currentUserId
            );

            trungLapBaoCaoService.recalculateTrust(
                    existingReport
            );

            reportRepository.save(existingReport);

            if (currentReporter != null) {

                userRewardService.rewardUser(
                        currentUserId,
                        2,
                        5
                );
            }

            realtimeService.broadcastReport(
                    suCoMapper.convertToDto(existingReport)
            );

            ai.setDistance(matchedDistance);

            log.info(
                    "\nNgười dùng {} đóng góp vào báo cáo ID: {} của {}"
                            + "\nLoại sự cố: {}, cách đó {}m,"
                            + "\nĐiểm tích lũy người đóng góp {}: {}",

                    maskUid(uid),
                    existingReport.getId(),

                    maskUid(existingReport.getReporter().getUid()),

                    existingReport.getLoaiSuCo().getTen(),

                    matchedDistance,

                    maskUid(uid),
                    currentReporter.getTotalPoints()
            );

            log.info(
                    "\nĐộ tin cậy mới của báo cáo ID {}: {}",
                    existingReport.getId(),
                    existingReport.getDoTinCay()
            );

            return ai;
        }

        report.setDiaChi(
                geocodingService.getAddress(
                        report.getViDo(),
                        report.getKinhDo()
                )
        );

        if (base64FullData != null
                && !base64FullData.isBlank()) {

            report.setHinhAnhUrl(
                    imageStorageService.saveBase64Image(
                            base64FullData
                    )
            );
        }

        report.setTrangThaiDuyet("AI_APPROVED");
        report.setDoTinCay(0);
        report.setTrangThaiXuLy("CHO_XU_LY");

        report.setIdTruSoDeXuat(null);
        report.setIdTruSoTiepNhan(null);

        BaoCaoSuCo savedReport =
                reportRepository.save(report);

        trungLapBaoCaoService.recalculateTrust(
                savedReport
        );

        reportRepository.save(savedReport);

        User reporter =
                userRepository.findById(uid).orElse(null);

        if (reporter != null
                && savedReport.getReporter() != null) {

            userRewardService.rewardUser(
                    uid,
                    5,
                    10
            );
        }

        log.info(
                "\nNgười dùng {} đã gửi báo cáo mới với ID: {}"
                        + "\nLoại: {}, độ tin cậy: {}, điểm tích lũy: {}",

                maskUid(uid),

                savedReport.getId(),

                savedReport.getLoaiSuCo().getTen(),

                savedReport.getDoTinCay(),

                savedReport.getReporter().getTotalPoints()
        );

        realtimeService.broadcastReport(
                suCoMapper.convertToDto(savedReport)
        );

        realtimeService.broadcastAdminNotification(
                "Có báo cáo mới chờ duyệt: "
                        + savedReport.getMoTa()
        );

        return ai;
    }

    @Transactional
    public ResponseEntity<?> cancelReport(
            Long reportId,
            String currentUid
    ) {

        BaoCaoSuCo report =
                reportRepository.findById(reportId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Không tìm thấy báo cáo"
                                ));

        log.info(
                "\n[HUY BAO CAO - BẮT ĐẦU]"
                        + "\nUser: {}"
                        + "\nReport ID: {}"
                        + "\nTrang thai xu ly: {}"
                        + "\nTrang thai duyet: {}\n",

                maskUid(currentUid),
                reportId,
                report.getTrangThaiXuLy(),
                report.getTrangThaiDuyet()
        );

        quyenHanService.checkOwner(
                report,
                currentUid
        );

        String xuLy =
                report.getTrangThaiXuLy();

        String duyet =
                report.getTrangThaiDuyet();

        if ("VERIFIED".equals(duyet)) {

            log.warn(
                    "\n[HUY BAO CAO - BỊ CHẶN]"
                            + "\nUser: {}"
                            + "\nReport ID: {}"
                            + "\nLý do: Đã VERIFIED\n",

                    maskUid(currentUid),
                    reportId
            );

            return ResponseEntity.badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    "Báo cáo đã được duyệt, không thể hủy."
                            )
                    );
        }

        if (!"CHO_XU_LY".equals(xuLy)) {

            return ResponseEntity.badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    "Không thể hủy báo cáo ở trạng thái hiện tại."
                            )
                    );
        }

        report.setTrangThaiXuLy("HUY_BO");

        BaoCaoSuCo saved =
                reportRepository.save(report);

        SuCoMapDto dto =
                suCoMapper.convertToDto(saved);

        dto.setDiaChi(saved.getDiaChi());

        realtimeService.broadcastReport(dto);

        realtimeService.refreshUserHistory(
                currentUid
        );

        log.info(
                "\n[HUY BAO CAO - THÀNH CÔNG]"
                        + "\nUser: {}"
                        + "\nReport ID: {}"
                        + "\nXu ly cu: {}"
                        + "\nDuyet: {}"
                        + "\nXu ly moi: HUY_BO\n",

                maskUid(currentUid),
                reportId,
                xuLy,
                duyet
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Đã hủy báo cáo thành công"
                )
        );
    }

   
}