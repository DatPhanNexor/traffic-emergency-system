package com.example.suco.service;

import com.example.suco.dto.SuCoMapDto;
import com.example.suco.dto.TruSoMapDto;
import com.example.suco.model.*;
import com.example.suco.repository.*;
import com.example.suco.util.GeocodingUtil;

import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.*;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
@Service
public class BaoCaoSuCoService {

    @Autowired
    private BaoCaoSuCoRepository reportRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LoaiSuCoRepository loaiSuCoRepository;
    @Autowired
    private GeminiAiService geminiAiService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private TruSoService truSoService;
    @Autowired
    private TruSoRepository truSoRepository;
    @Autowired
    private SpamRepository spamRepository;
    @Autowired
    private BaoCaoTrungLapRepository baoCaoTrungLapRepository;
    @Autowired
    private MuaGoiRepository muaGoiRepository;
    // --- HÀM CHUYỂN ĐỔI DTO DÙNG CHUNG ---
    @Autowired
    private GeocodingUtil geocodingUtil; // Inject util định vị
    private static final Logger log = LoggerFactory.getLogger(LoaiSuCoService.class);
    public TruSo findById(Long id) {
    return truSoRepository.findById(id).orElse(null);
}
    // Cập nhật hàm convert để lấy thêm trường diaChi từ Entity
    // --- HÀM CHUYỂN ĐỔI DTO DÙNG CHUNG ---
    // Sửa lại hàm convertToDto trong BaoCaoSuCoApiController
    private SuCoMapDto convertToDto(BaoCaoSuCo b) {
    String tenLoai = (b.getLoaiSuCo() != null) ? b.getLoaiSuCo().getTen() : "Không xác định";
    String iconUrl = (b.getLoaiSuCo() != null) ? b.getLoaiSuCo().getIconUrl() : "";
    String tenNguoiBao = (b.getReporter() != null) ? b.getReporter().getName() : "Người dân báo";

    // ✅ tạo dto trước
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
            null, null, null, null,
            b.getDiaChi(),
            tenNguoiBao
    );

    if (b.getReporter() != null) {
    dto.setReporterUid(b.getReporter().getUid());
    }

    if (b.getIdTruSoDeXuat() != null) {
        TruSo ts = truSoRepository.findById(b.getIdTruSoDeXuat()).orElse(null);

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
        TruSo ts = truSoRepository.findById(b.getIdTruSoTiepNhan()).orElse(null);

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
public List<SuCoMapDto> getMyReports(String uid) {
    return reportRepository.findByReporterUid(uid)
            .stream()
            .map(this::convertToDto)
            .toList();
}
    public List<SuCoMapDto> getPendingReportsForAdmin() {
    return reportRepository.findPendingReportsForAdmin()
            .stream()
            .map(this::convertToDto)
            .toList();
}

    // Hàm che bớt UID khi log để bảo vệ thông tin cá nhân
    private String maskUid(String uid) {
        if (uid == null)
            return "null";
        return uid.length() > 5 ? uid.substring(0, 5) + "***" : uid + "***";
    }

    private void recalculateTrust(BaoCaoSuCo report) {
        int trust = baoCaoTrungLapRepository.countByBaoCaoId(report.getId()) + 1;
        report.setDoTinCay(trust);
    }

    @Transactional
    public AiVerifyResult submitReport(String uid, BaoCaoSuCo report, String base64FullData) {
        LoaiSuCo loaiSuCo = loaiSuCoRepository.findById(report.getLoaiSuCo().getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Loại sự cố không tồn tại"));
        report.setLoaiSuCo(loaiSuCo);

        AiVerifyResult ai;

        if (base64FullData != null && !base64FullData.isBlank()) {
            ai = geminiAiService.verifyImage(base64FullData, loaiSuCo.getTen());
            System.out.println("=== AI RESULT ===");
            System.out.println("Valid: " + ai.isValid());
            System.out.println("Confidence: " + ai.getConfidence());
            System.out.println("Distance (before): " + ai.getDistance());
        } else {
            ai = new AiVerifyResult(true, 50, "Không có ảnh", null);
        }

        if (!ai.isValid())
            return ai;

        List<BaoCaoSuCo> activeReports = reportRepository.findByTrangThaiXuLyNotIn(
                List.of("HOAN_THANH", "HUY_BO"));
        BaoCaoSuCo existingReport = null;

        Double matchedDistance = null;

        for (BaoCaoSuCo ex : activeReports) {
            double distanceMeters = calculateDistance(
                    report.getViDo(), report.getKinhDo(),
                    ex.getViDo(), ex.getKinhDo());

            if (distanceMeters <= 20 && ex.getLoaiSuCo().getId().equals(loaiSuCo.getId())) {
                existingReport = ex;
                matchedDistance = distanceMeters; // 👈 lưu lại
                break;
            }
        }

        String currentUserId = uid;
        User currentReporter = userRepository.findById(uid).orElse(null);

        // ================== CASE TRÙNG ==================
        if (existingReport != null) {
            System.out.println("Bắt gặp báo cáo trùng lập với ID: " + existingReport.getId());

            //1. Nếu là chính chủ report → bỏ qua
            if (existingReport.getReporter().getUid().equals(uid)) {
                AiVerifyResult result = new AiVerifyResult(
                        false,
                        100,
                        "Bạn đã báo cáo sự cố này trước đó");
                result.setDistance(matchedDistance);
                  log.info("\n[TRÙNG - CHÍNH CHỦ]" +
            "\nUser: {}" +
            "\nReport ID: {}" +
            "\nLoại sự cố: {}" +
            "\nKhoảng cách: {} m" +
            "\nĐộ tin cậy report hiện tại: {}" +
            "\nĐiểm hiện tại của user: {}\n",
            maskUid(uid),
            existingReport.getId(),
            existingReport.getLoaiSuCo().getTen(),
            matchedDistance,
            existingReport.getDoTinCay(),  
            currentReporter != null ? currentReporter.getTotalPoints() : "N/A"
    );
                return result;

            }

            // // 2. Nếu đã từng góp → bỏ qua
            boolean existed =
            baoCaoTrungLapRepository.existsByBaoCao_IdAndUserId(existingReport.getId(),
            uid);;

            if (existed) {
            AiVerifyResult result = new AiVerifyResult(
            false,
            100,
            "Bạn đã báo cáo sự cố này trước đó"
            );
            result.setDistance(matchedDistance);
            return result;
            }
            // Lưu user góp
            baoCaoTrungLapRepository.save(
                    new BaoCaoTrungLap(existingReport, currentUserId));

            // tính lại từ DB
            recalculateTrust(existingReport);
            reportRepository.save(existingReport);

            // 5. Cộng điểm
            if (currentReporter != null) {
                int pointsToAdd = isUserVip(currentUserId) ? 5 : 2;
                currentReporter.setTotalPoints(currentReporter.getTotalPoints() + pointsToAdd);
                userRepository.save(currentReporter);

                messagingTemplate.convertAndSend(
                        "/topic/user-stats/" + currentUserId,
                        currentReporter);
            }

            messagingTemplate.convertAndSend("/topic/su-co", convertToDto(existingReport));

            ai.setDistance(matchedDistance);
            log.info("\nNgười dùng {} đóng góp vào báo cáo ID: {} của {}\nLoại sự cố: {}, cách đó {}m,\nĐiểm tích lũy người đóng góp {}: {}",
                    maskUid(uid), existingReport.getId(),
                    maskUid(existingReport.getReporter().getUid()),
                    existingReport.getLoaiSuCo().getTen(),
                    matchedDistance,
                    maskUid(uid),currentReporter.getTotalPoints() );
            log.info("\nĐộ tin cậy mới của báo cáo ID {}: {}", existingReport.getId(), existingReport.getDoTinCay());
            return ai;
        }

        try {
            var addrMap = geocodingUtil.getAddressFromCoordinates(report.getViDo(), report.getKinhDo());
            report.setDiaChi(geocodingUtil.formatAddress(addrMap));
        } catch (Exception e) {
            report.setDiaChi("Tọa độ: " + report.getViDo() + ", " + report.getKinhDo());
        }
        if (base64FullData != null && !base64FullData.isBlank()) {
            report.setHinhAnhUrl(saveBase64Image(base64FullData));
        }
        report.setTrangThaiDuyet("AI_APPROVED");
        report.setDoTinCay(0);
        report.setTrangThaiXuLy("CHO_XU_LY");

        // QUAN TRỌNG: Đảm bảo 2 cột này NULL khi người dùng vừa gửi
        report.setIdTruSoDeXuat(null);
        report.setIdTruSoTiepNhan(null);

        BaoCaoSuCo savedReport = reportRepository.save(report);
        recalculateTrust(savedReport);
        reportRepository.save(savedReport);

        User reporter = userRepository.findById(uid).orElse(null);
if (reporter != null && savedReport.getReporter() != null) {
    int pointsToAdd = isUserVip(uid) ? 10 : 5;

    reporter.setTotalPoints(reporter.getTotalPoints() + pointsToAdd);
    userRepository.save(reporter);

    messagingTemplate.convertAndSend(
        "/topic/user-stats/" + uid,
        reporter
    );
}

        log.info("\nNgười dùng {} đã gửi báo cáo mới với ID: {}\nLoại: {}, độ tin cậy: {}, điểm tích lũy: {}",
                maskUid(uid), savedReport.getId(), 
                savedReport.getLoaiSuCo().getTen(), 
                savedReport.getDoTinCay(), 
                savedReport.getReporter().getTotalPoints()
                );

        messagingTemplate.convertAndSend("/topic/su-co", convertToDto(savedReport));
        messagingTemplate.convertAndSend("/topic/admin-notifications",
                "Có báo cáo mới chờ duyệt: " + savedReport.getMoTa());

        return ai;
    }
    
    
@Transactional
public ResponseEntity<?> cancelReport(Long reportId, String currentUid) {

    BaoCaoSuCo report = reportRepository.findById(reportId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy báo cáo"));

        log.info("\n[HUY BAO CAO - BẮT ĐẦU]" +
        "\nUser: {}" +
        "\nReport ID: {}" +
        "\nTrang thai xu ly: {}" +
        "\nTrang thai duyet: {}\n",
        maskUid(currentUid),
        reportId,
        report.getTrangThaiXuLy(),
        report.getTrangThaiDuyet()
);

    // 1. Check chính chủ
    if (report.getReporter() == null ||
        !report.getReporter().getUid().equals(currentUid)) {

        return ResponseEntity.status(403)
                .body(Map.of("message", "Bạn không có quyền hủy báo cáo này."));
    }

    String xuLy = report.getTrangThaiXuLy();
    String duyet = report.getTrangThaiDuyet();

    // ❌ CHẶN nếu đã được admin duyệt
if ("VERIFIED".equals(duyet)) {

    log.warn("\n[HUY BAO CAO - BỊ CHẶN]" +
            "\nUser: {}" +
            "\nReport ID: {}" +
            "\nLý do: Đã VERIFIED\n",
            maskUid(currentUid),
            reportId
    );

    return ResponseEntity.badRequest()
            .body(Map.of("message", "Báo cáo đã được duyệt, không thể hủy."));
}

    // 3. Chỉ cho hủy khi CHO_XU_LY
    if (!"CHO_XU_LY".equals(xuLy)) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Không thể hủy báo cáo ở trạng thái hiện tại."));
    }

    // 4. Update trạng thái
    report.setTrangThaiXuLy("HUY_BO");
    BaoCaoSuCo saved = reportRepository.save(report);

    // 5. DTO realtime (giữ nguyên style bạn)
    SuCoMapDto dto = convertToDto(saved);
    dto.setDiaChi(saved.getDiaChi());

    messagingTemplate.convertAndSend("/topic/su-co", dto);
    messagingTemplate.convertAndSend(
            "/topic/user/" + currentUid + "/history",
            "REFRESH"
    );


    log.info("\n[HUY BAO CAO - THÀNH CÔNG]" +
        "\nUser: {}" +
        "\nReport ID: {}" +
        "\nXu ly cu: {}" +
        "\nDuyet: {}" +
        "\nXu ly moi: HUY_BO\n",
        maskUid(currentUid),
        reportId,
        xuLy,
        duyet
);

    return ResponseEntity.ok(Map.of("message", "Đã hủy báo cáo thành công"));
}
    @Transactional
    public BaoCaoSuCo submitAdminReport(BaoCaoSuCo report, MultipartFile image) {
        LoaiSuCo loaiSuCo = loaiSuCoRepository.findById(report.getLoaiSuCo().getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Loại sự cố không tồn tại"));
        report.setLoaiSuCo(loaiSuCo);

        User adminUser = userRepository.findById("ADMIN_SYSTEM")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ADMIN_SYSTEM"));
        report.setReporter(adminUser);
        report.setNguonBaoCao("ADMIN");
        report.setAiXacNhan(true);
        report.setTrangThaiDuyet("VERIFIED");
        if (report.getMucDoNghiemTrong() == null)
            report.setMucDoNghiemTrong("PENDING");

        if (image != null && !image.isEmpty()) {
            report.setHinhAnhUrl(saveMultipartImage(image));
        }

        TruSo ganNhat = truSoService.timTruSoGanNhat(report.getViDo(), report.getKinhDo());
        if (ganNhat != null) {
            report.setIdTruSoDeXuat(ganNhat.getId());
            report.setTrangThaiXuLy("CHO_XU_LY");
        }

        BaoCaoSuCo savedReport = reportRepository.save(report);

        // Gửi WebSocket (Đã dọn dẹp dòng thừa)
        messagingTemplate.convertAndSend("/topic/su-co", convertToDto(savedReport));
        if (ganNhat != null) {
            messagingTemplate.convertAndSend("/topic/tru-so/" + ganNhat.getId() + "/su-co", convertToDto(savedReport));
        }

        return savedReport;
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
                // Thường +5, Gói +10
                int pointsToAdd = isUserVip(reporter.getUid()) ? 10 : 5;
                reporter.setTotalPoints(reporter.getTotalPoints() + pointsToAdd);

                userRepository.save(reporter);
                messagingTemplate.convertAndSend("/topic/user-stats/" + reporter.getUid(), reporter);
            }

            TruSo ganNhat = truSoService.timTruSoGanNhat(report.getViDo(), report.getKinhDo());
            if (ganNhat != null) {
                report.setIdTruSoDeXuat(ganNhat.getId());
                // report.setIdTruSoTiepNhan(null);
            }

            // Lưu cập nhật cho báo cáo đúng
            BaoCaoSuCo updatedReport = reportRepository.save(report);

            // Gửi WebSocket cập nhật Marker
            messagingTemplate.convertAndSend("/topic/su-co", convertToDto(updatedReport));

            // Thông báo cho trụ sở
            if (updatedReport.getIdTruSoDeXuat() != null) {
                messagingTemplate.convertAndSend(
                        "/topic/tru-so/" + updatedReport.getIdTruSoDeXuat() + "/su-co",
                        convertToDto(updatedReport));
            }
            if (updatedReport.getIdTruSoTiepNhan() != null) {
                messagingTemplate.convertAndSend(
                        "/topic/tru-so/" + updatedReport.getIdTruSoTiepNhan() + "/su-co",
                        convertToDto(updatedReport));
            }
        } else {
            // --- TRƯỜNG HỢP SPAM ---
            // 1. Copy sang bảng Spam
            Spam spam = new Spam(report);
            spamRepository.save(spam);

            // 2. Cập nhật chỉ số Spam cho User
            if (reporter != null && !"ADMIN".equals(report.getNguonBaoCao())) {
                reporter.setSpamCount(reporter.getSpamCount() + 1);
                userRepository.save(reporter);
                messagingTemplate.convertAndSend("/topic/user-stats/" + reporter.getUid(), reporter);
            }

            // 3. Báo realtime cho các client đang nghe /topic/su-co trước khi xóa
            report.setTrangThaiDuyet("REJECTED");
            report.setTrangThaiXuLy("REJECTED");
            messagingTemplate.convertAndSend("/topic/su-co", convertToDto(report));

            // 4. Xóa vĩnh viễn khỏi bảng sự cố
            reportRepository.delete(report);

            // 5. Bắn socket lệnh XÓA để App Android ẩn Marker ngay lập tức
            messagingTemplate.convertAndSend("/topic/su-co-delete", reportId);

            // QUAN TRỌNG: Kết thúc hàm tại đây, không chạy xuống phần save bên dưới
            return;
        }
    }

    // Hàm helper kiểm tra trạng thái gói
    private boolean isUserVip(String userId) {
        return muaGoiRepository.findFirstByUserIdAndTrangThaiAndNgayHetHanAfter(
                userId, "ACTIVE", java.time.LocalDateTime.now()).isPresent();
    }

    @Transactional
    public void updateProcessStatus(Long reportId, String status, Long idTruSoThucTe) {
        BaoCaoSuCo report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo"));

        report.setTrangThaiXuLy(status);

        // Gán trụ sở thực tế tiếp nhận (giống SOS)
        if ("DANG_XU_LY".equals(status)) {
            report.setIdTruSoTiepNhan(idTruSoThucTe);
        }
        BaoCaoSuCo saved = reportRepository.save(report);

        // Đồng bộ lại Marker trên bản đồ admin và trụ sở
        messagingTemplate.convertAndSend("/topic/su-co", convertToDto(saved));

        if (saved.getIdTruSoTiepNhan() != null) {
            messagingTemplate.convertAndSend("/topic/tru-so/" + saved.getIdTruSoTiepNhan() + "/su-co",
                    convertToDto(saved));
        }
    }
   @Transactional
public Map<String, Object> updateSuCoStatus(Long id, String status, TruSo current) {

    BaoCaoSuCo suCo = reportRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy sự cố"
            ));

    String currentStatus = suCo.getTrangThaiXuLy();
    Long currentTiepNhanId = suCo.getIdTruSoTiepNhan();

    // ===== 1. VALIDATE INPUT =====
    if (status == null || status.trim().isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status không được để trống");
    }

    List<String> valid = List.of("CHO_XU_LY", "DANG_XU_LY", "HOAN_THANH", "HUY_BO");
    if (!valid.contains(status)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ");
    }

    // ===== 2. TERMINAL STATE =====
    if ("HOAN_THANH".equals(currentStatus) || "HUY_BO".equals(currentStatus)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Sự cố đã kết thúc, không thể thay đổi trạng thái!");
    }

    // ===== 3. SAME STATUS =====
    if (status.equals(currentStatus)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Trạng thái đã là giá trị hiện tại");
    }

    // ===== 4. KHÔNG CHO QUAY LẠI CHO_XU_LY =====
    if ("CHO_XU_LY".equals(status)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Không được quay lại trạng thái CHỜ XỬ LÝ!");
    }

    // ===== 5. STATE MACHINE =====

    // CHO_XU_LY -> DANG_XU_LY
    if ("DANG_XU_LY".equals(status)) {
        if (!"CHO_XU_LY".equals(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ được tiếp nhận từ trạng thái CHỜ XỬ LÝ!");
        }

        suCo.setIdTruSoTiepNhan(current.getId());
        suCo.setIdTruSoDeXuat(null);
    }

    // DANG_XU_LY -> HOAN_THANH
    else if ("HOAN_THANH".equals(status)) {
        if (!"DANG_XU_LY".equals(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Phải đang xử lý mới được hoàn thành.");
        }

        if (!current.getId().equals(currentTiepNhanId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền hoàn thành sự cố này!");
        }
    }

    // CHO_XU_LY -> HUY_BO
    else if ("HUY_BO".equals(status)) {
        if (!"CHO_XU_LY".equals(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ được hủy khi đang CHỜ XỬ LÝ!");
        }
    }

    // ===== SAVE =====
    suCo.setTrangThaiXuLy(status);
    BaoCaoSuCo saved = reportRepository.save(suCo);

    SuCoMapDto dto = convertToDto(saved);

    // ===== SOCKET =====
    if (saved.getIdTruSoTiepNhan() != null) {
        messagingTemplate.convertAndSend(
                "/topic/tru-so/" + saved.getIdTruSoTiepNhan() + "/su-co",
                dto
        );
    }

    messagingTemplate.convertAndSend("/topic/su-co", dto);

    if (saved.getReporter() != null) {
        messagingTemplate.convertAndSend(
                "/topic/user/" + saved.getReporter().getUid() + "/history",
                "REFRESH"
        );
    }

    return Map.of(
            "message", "Cập nhật trạng thái thành công",
            "newStatus", status
    );
}
    @Transactional
public Map<String, Object> capNhatMucDo(Long id, String mucDo, TruSo current) {

    BaoCaoSuCo suCo = reportRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Không tìm thấy sự cố"));

    log.info("\nTrụ sở {} cập nhật mức độ {} sự cố: {}"
    +"\nTrạng thái xử lý hiện tại: {}"
    +"\nMức độ hiện tại: {}"
    +"\nTrụ sở tiếp nhận hiện tại: {}",
    current.getTenTruSo(), mucDo, id, 
    suCo.getTrangThaiXuLy(),
    suCo.getMucDoNghiemTrong(),
    suCo.getIdTruSoTiepNhan());

    String status = suCo.getTrangThaiXuLy();

    // Không cho update nếu đã kết thúc
    if ("HOAN_THANH".equals(status) || "HUY_BO".equals(status)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Sự cố đã kết thúc, không thể thay đổi mức độ nghiêm trọng!");
    }

if (!"DANG_XU_LY".equals(status)) {
    log.error("\nSự cố ID {} đang ở trạng thái '{}', không cho phép cập nhật mức độ!", id, status);
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Chỉ được cập nhật mức độ khi sự cố đang xử lý!");
}

Long idTruSo = suCo.getIdTruSoTiepNhan();
if (idTruSo == null) {
    log.error("\nSự cố ID {} chưa có trụ sở tiếp nhận, không thể cập nhật mức độ!", id);
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Sự cố chưa được tiếp nhận!");
}

    // Check quyền
    if (!current.getId().equals(suCo.getIdTruSoTiepNhan())) {
        log.error("\nSự cố ID {} không thuộc trụ sở {}!", id, current.getTenTruSo());
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Bạn không có quyền chỉnh sửa mức độ của sự cố này!");
    }

    // Validate mucDo
    if (mucDo == null || mucDo.trim().isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Mức độ không được để trống!");
    }

    List<String> validLevels = List.of("LOW", "MEDIUM", "HIGH");
    if (!validLevels.contains(mucDo.toUpperCase())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Mức độ không hợp lệ! (LOW, MEDIUM, HIGH)");
    }

    // Normalize
    mucDo = mucDo.toUpperCase();

    // Update
    suCo.setMucDoNghiemTrong(mucDo);
    BaoCaoSuCo saved = reportRepository.save(suCo);

    SuCoMapDto dto = convertToDto(saved);

    log.info("\nCập nhật mức độ thành công. Mức độ mới: {}"
            +"\nID sự cố: {}"
            +"\nTrụ sở tiếp nhận: {}"
            +"\nTrạng thái xử lý: {}",
            suCo.getMucDoNghiemTrong(), id, suCo.getIdTruSoTiepNhan(), suCo.getTrangThaiXuLy());

            if (saved.getIdTruSoTiepNhan() != null) {
        messagingTemplate.convertAndSend(
                "/topic/tru-so/" + saved.getIdTruSoTiepNhan() + "/su-co",
                dto
        );
    }

    messagingTemplate.convertAndSend("/topic/su-co", dto);

    return Map.of(
            "message", "Cập nhật mức độ thành công",
            "mucDoMoi", mucDo
    );


}
    // --- tinh khoang cach ---
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371e3;
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String saveBase64Image(String base64Data) {
        try {
            String fileName = System.currentTimeMillis() + "_report.jpg";
            Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads", "reports");
            if (!Files.exists(uploadPath))
                Files.createDirectories(uploadPath);
            String base64Image = base64Data.contains(",") ? base64Data.split(",")[1] : base64Data;
            Files.write(uploadPath.resolve(fileName), Base64.getDecoder().decode(base64Image));
            return "/uploads/reports/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String saveMultipartImage(MultipartFile image) {
        try {
            String filename = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "reports");
            if (!Files.exists(uploadDir))
                Files.createDirectories(uploadDir);
            Files.copy(image.getInputStream(), uploadDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/reports/" + filename;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}