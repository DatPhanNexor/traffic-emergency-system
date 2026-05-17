package com.example.suco.service.suco.baocao.admin;

import com.example.suco.dto.SuCoMapDto;
import com.example.suco.model.BaoCaoSuCo;
import com.example.suco.model.Spam;
import com.example.suco.model.TruSo;
import com.example.suco.model.User;
import com.example.suco.repository.BaoCaoSuCoRepository;
import com.example.suco.repository.SpamRepository;
import com.example.suco.repository.UserRepository;
import com.example.suco.service.TruSoService;
import com.example.suco.service.suco.baocao.system.mapper.SuCoMapper;
import com.example.suco.service.suco.baocao.system.realtime.BaoCaoRealtimeService;
import com.example.suco.service.suco.baocao.system.reward.UserRewardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DuyetSuCoService {

    @Autowired
    private BaoCaoSuCoRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpamRepository spamRepository;

    @Autowired
    private TruSoService truSoService;

    @Autowired
    private UserRewardService userRewardService;

    @Autowired
    private BaoCaoRealtimeService realtimeService;

    @Autowired
    private SuCoMapper suCoMapper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final Logger log =
        LoggerFactory.getLogger(DuyetSuCoService.class);

    public List<SuCoMapDto> getPendingReportsForAdmin() {

    List<BaoCaoSuCo> reports = reportRepository.findPendingReportsForAdmin();

    log.info("Đang tải {} báo cáo chờ duyệt", reports.size());

    return reports.stream()
            .map(suCoMapper::convertToDto)
            .toList();
}

    @Transactional
    public void verifyReport(Long reportId, boolean isCorrect) {

        BaoCaoSuCo report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy báo cáo"
                ));

        User reporter = report.getReporter();

        if (isCorrect) {

            report.setTrangThaiDuyet("VERIFIED");

            if (reporter != null && !"ADMIN".equals(report.getNguonBaoCao())) {

                int pointsToAdd =
                        userRewardService.isUserVip(reporter.getUid()) ? 10 : 5;

                reporter.setTotalPoints(
                        reporter.getTotalPoints() + pointsToAdd);

                userRepository.save(reporter);

                messagingTemplate.convertAndSend(
                        "/topic/user-stats/" + reporter.getUid(),
                        reporter
                );
            }

            TruSo ganNhat = truSoService.timTruSoGanNhat(
                    report.getViDo(),
                    report.getKinhDo());

            if (ganNhat != null) {
                report.setIdTruSoDeXuat(ganNhat.getId());
            }

            BaoCaoSuCo updatedReport = reportRepository.save(report);

            realtimeService.broadcastReport(
                    suCoMapper.convertToDto(updatedReport));

            if (updatedReport.getIdTruSoDeXuat() != null) {

                realtimeService.broadcastTruSo(
                        updatedReport.getIdTruSoDeXuat(),
                        suCoMapper.convertToDto(updatedReport)
                );
            }

            if (updatedReport.getIdTruSoTiepNhan() != null) {

                realtimeService.broadcastTruSo(
                        updatedReport.getIdTruSoTiepNhan(),
                        suCoMapper.convertToDto(updatedReport)
                );
            }

        } else {

            Spam spam = new Spam(report);
            spamRepository.save(spam);

            if (reporter != null && !"ADMIN".equals(report.getNguonBaoCao())) {

                reporter.setSpamCount(
                        reporter.getSpamCount() + 1);

                userRepository.save(reporter);

                messagingTemplate.convertAndSend(
                        "/topic/user-stats/" + reporter.getUid(),
                        reporter
                );
            }

            report.setTrangThaiDuyet("REJECTED");
            report.setTrangThaiXuLy("REJECTED");

            realtimeService.broadcastReport(
                    suCoMapper.convertToDto(report));

            reportRepository.delete(report);

            realtimeService.broadcastDelete(reportId);

            return;
        }
    }
    
}