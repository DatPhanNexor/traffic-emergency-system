package com.example.suco.service;

import com.example.suco.dto.TinHieuSOSRequestDTO;
import com.example.suco.model.MuaGoi;
import com.example.suco.model.TinHieuSOS;
import com.example.suco.model.TruSo;
import com.example.suco.repository.MuaGoiRepository;
import com.example.suco.repository.TinHieuSOSRepository;
import com.example.suco.service.DieuPhoiSOSService.ThongTinDieuPhoi;
import com.example.suco.util.GeocodingUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TinHieuSOSService {

    private static final Logger log = LoggerFactory.getLogger(TinHieuSOSService.class);

    private static final String STATUS_CHO_XU_LY = "CHO_XU_LY";
    private static final String STATUS_DANG_XU_LY = "DANG_XU_LY";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_TRANG_THAI = "trangThai";
    private static final String FIELD_ID_SOS = "idSOS";
    private static final String FIELD_LOAI_THONG_BAO = "loaiThongBao";
    private static final String FIELD_ID_SOS_LOWER = "idSos";
    private static final String FIELD_REASON = "reason";

    private static final String RESULT_SOS_DATA = "sosData";
    private static final String RESULT_TRU_SO_GAN_NHAT = "truSoGanNhat";
    private static final String RESULT_THONG_TIN_DIEU_PHOI = "thongTinDieuPhoi";

    private static final String TOPIC_ADMIN_SOS = "/topic/admin/sos";
    private static final String TOPIC_TRU_SO_PREFIX = "/topic/truso/";
    private static final String TOPIC_USER_PREFIX = "/topic/user/";
    private static final String TOPIC_SOS_STATUS_SUFFIX = "/sos-status";
    private static final String TOPIC_HISTORY_SUFFIX = "/history";
    private static final String TOPIC_DIEU_PHOI_SUFFIX = "/dieu-phoi";

    private static final String MSG_VIP_ASSIGNED_PREFIX = "Bạn có gói đặc quyền! Trụ sở ";
    private static final String MSG_VIP_ASSIGNED_SUFFIX = " đang đến ngay.";
    private static final String MSG_FALLBACK_ADDRESS = "Yêu cầu cứu hộ tại: ";
    private static final String MSG_REFRESH = "REFRESH";

    private static final String NOTIFY_DELETE_SOS = "XOA_SOS";
    private static final String REASON_VIP_OVERRIDE = "VIP_GHI_DE";

    private static final String IMAGE_PREFIX = "sos_img";
    private static final String AUDIO_PREFIX = "sos_audio";
    private static final String AUDIO_EXTENSION = ".m4a";
    private static final String IMAGE_EXTENSION = ".jpg";
    private static final String UPLOAD_FOLDER = "uploads";
    private static final String SOS_FOLDER = "sos";
    private static final String UPLOAD_URL_PREFIX = "/uploads/sos/";

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private TinHieuSOSRepository tinHieuSOSRepository;

    @Autowired
    private MuaGoiRepository muaGoiRepository;

    @Autowired
    private TruSoService truSoService;

    @Autowired
    private GeocodingUtil geocodingUtil;

    @Autowired
    private DieuPhoiSOSService dieuPhoiService;

    @Transactional
    public Map<String, Object> xuLyTinHieuSOS(String uid, TinHieuSOSRequestDTO dto) {
        validateRequestDto(dto);

        Optional<TinHieuSOS> existingActiveSos = findExistingActiveSos(uid);

        if (existingActiveSos.isPresent()) {
            TinHieuSOS sosDaCo = syncExistingActiveSosWithRequest(
                    existingActiveSos.get(),
                    dto
            );

            Map<String, Object> ketQua = new HashMap<>();
            ketQua.put(RESULT_SOS_DATA, sosDaCo);
            ketQua.put(RESULT_TRU_SO_GAN_NHAT, null);
            ketQua.put(RESULT_THONG_TIN_DIEU_PHOI, null);

            return ketQua;
        }

        TinHieuSOS sos = createBaseSos(uid, dto);

        setAddress(sos, dto);
        setAttachmentFiles(sos, dto);

        sos.setTrangThai(STATUS_CHO_XU_LY);
        TinHieuSOS sosDaLuu = tinHieuSOSRepository.save(sos);

        boolean laVip = isVipUser(uid);
        sosDaLuu.setIsVip(laVip);

        ThongTinDieuPhoi thongTinDieuPhoi = dieuPhoiService.khoiTaoDieuPhoi(sosDaLuu);
        TruSo truSoGanNhat = getTruSoGanNhat(thongTinDieuPhoi);

        if (truSoGanNhat != null) {
            sosDaLuu.setIdTruSoDeXuat(truSoGanNhat.getId());
        }

        if (laVip && truSoGanNhat != null) {
            handleVipSos(sosDaLuu, truSoGanNhat, thongTinDieuPhoi);
        }

        sosDaLuu = tinHieuSOSRepository.save(sosDaLuu);

        sendFinalRealtimeMessages(sosDaLuu);

        Map<String, Object> ketQua = new HashMap<>();
        ketQua.put(RESULT_SOS_DATA, sosDaLuu);
        ketQua.put(RESULT_TRU_SO_GAN_NHAT, truSoGanNhat);
        ketQua.put(RESULT_THONG_TIN_DIEU_PHOI, thongTinDieuPhoi);

        return ketQua;
    }

    private void validateRequestDto(TinHieuSOSRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Dữ liệu SOS không được để trống");
        }
    }

    private Optional<TinHieuSOS> findExistingActiveSos(String uid) {
        List<String> trangThaiDangXuLy = List.of(
                STATUS_CHO_XU_LY,
                STATUS_DANG_XU_LY
        );

        return tinHieuSOSRepository.findFirstByUserIdAndTrangThaiInOrderByCreatedAtDesc(
                uid,
                trangThaiDangXuLy
        );
    }

    private TinHieuSOS syncExistingActiveSosWithRequest(
            TinHieuSOS sosDaCo,
            TinHieuSOSRequestDTO dto
    ) {
        boolean changed = false;

        String requestGhiChu = resolveGhiChu(dto);

        if (requestGhiChu != null && !requestGhiChu.isBlank()) {
            sosDaCo.setGhiChu(requestGhiChu);
            changed = true;
        }

        if (dto.getDiaChi() != null && !dto.getDiaChi().isBlank()) {
            sosDaCo.setDiaChi(dto.getDiaChi());
            changed = true;
        }

        if (changed) {
            return tinHieuSOSRepository.save(sosDaCo);
        }

        return sosDaCo;
    }

    private TinHieuSOS createBaseSos(String uid, TinHieuSOSRequestDTO dto) {
        TinHieuSOS sos = new TinHieuSOS();

        sos.setUserId(uid);
        sos.setViDo(dto.getViDo());
        sos.setKinhDo(dto.getKinhDo());
        sos.setGhiChu(resolveGhiChu(dto));

        return sos;
    }

    private String resolveGhiChu(TinHieuSOSRequestDTO dto) {
        String ghiChu = dto.getGhiChu();

        if (ghiChu == null || ghiChu.isBlank()) {
            return dto.getMoTa();
        }

        return ghiChu;
    }

    private void setAddress(TinHieuSOS sos, TinHieuSOSRequestDTO dto) {
        if (dto.getDiaChi() != null && !dto.getDiaChi().isBlank()) {
            sos.setDiaChi(dto.getDiaChi());
            return;
        }

        try {
            Map<String, String> addrMap = geocodingUtil.getAddressFromCoordinates(
                    sos.getViDo(),
                    sos.getKinhDo()
            );
            sos.setDiaChi(geocodingUtil.formatAddress(addrMap));
        } catch (RuntimeException e) {
            sos.setDiaChi(MSG_FALLBACK_ADDRESS + sos.getViDo() + ", " + sos.getKinhDo());
        }
    }

    private void setAttachmentFiles(TinHieuSOS sos, TinHieuSOSRequestDTO dto) {
        if (dto.getHinhAnhBase64() != null && !dto.getHinhAnhBase64().isBlank()) {
            sos.setHinhAnh(saveBase64ToFile(dto.getHinhAnhBase64(), IMAGE_PREFIX));
        }

        if (dto.getGhiAmBase64() != null && !dto.getGhiAmBase64().isBlank()) {
            sos.setGhiAm(saveBase64ToFile(dto.getGhiAmBase64(), AUDIO_PREFIX));
        }
    }

    private boolean isVipUser(String uid) {
        log.info("=== DEBUG VIP USER {} ===", uid);

        List<MuaGoi> listGoi = muaGoiRepository.findByUserId(uid);
        boolean laVip = listGoi.stream()
                .anyMatch(mg -> STATUS_ACTIVE.equalsIgnoreCase(mg.getTrangThai()));

        log.info("VIP STATUS = {}", laVip);

        return laVip;
    }

    private TruSo getTruSoGanNhat(ThongTinDieuPhoi thongTinDieuPhoi) {
        if (thongTinDieuPhoi == null) {
            return null;
        }

        Long idTruSoDauTien = thongTinDieuPhoi.layIdTruSoHienTai();

        if (idTruSoDauTien == null) {
            return null;
        }

        return truSoService.timTruSoTheoId(idTruSoDauTien);
    }

    private void handleVipSos(
            TinHieuSOS sosDaLuu,
            TruSo truSoGanNhat,
            ThongTinDieuPhoi thongTinDieuPhoi
    ) {
        log.info(
                "Phát hiện User {} có gói đặc quyền. Tự động gán trụ sở {}",
                sosDaLuu.getUserId(),
                truSoGanNhat.getTenTruSo()
        );

        sosDaLuu.setIdTruSoTiepNhan(truSoGanNhat.getId());
        sosDaLuu.setTrangThai(STATUS_DANG_XU_LY);

        dieuPhoiService.danhDauDaTiepNhan(
                sosDaLuu.getId(),
                sosDaLuu.getIdTruSoTiepNhan()
        );

        sendVipRealtimeMessageToUser(sosDaLuu, truSoGanNhat);
        messagingTemplate.convertAndSend(
                TOPIC_TRU_SO_PREFIX + sosDaLuu.getIdTruSoTiepNhan(),
                sosDaLuu
        );

        notifyOtherTruSoToRemoveSos(sosDaLuu, thongTinDieuPhoi);
    }

    private void sendVipRealtimeMessageToUser(TinHieuSOS sosDaLuu, TruSo truSoGanNhat) {
        Map<String, Object> thongBaoApp = new HashMap<>();

        thongBaoApp.put(FIELD_ID_SOS, sosDaLuu.getId());
        thongBaoApp.put(FIELD_TRANG_THAI, STATUS_DANG_XU_LY);
        thongBaoApp.put(
                FIELD_MESSAGE,
                MSG_VIP_ASSIGNED_PREFIX + truSoGanNhat.getTenTruSo() + MSG_VIP_ASSIGNED_SUFFIX
        );

        messagingTemplate.convertAndSend(
                TOPIC_USER_PREFIX + sosDaLuu.getUserId() + TOPIC_SOS_STATUS_SUFFIX,
                thongBaoApp
        );
    }

    private void notifyOtherTruSoToRemoveSos(
            TinHieuSOS sosDaLuu,
            ThongTinDieuPhoi thongTinDieuPhoi
    ) {
        if (thongTinDieuPhoi == null || thongTinDieuPhoi.getDanhSachIdTruSo().size() <= 1) {
            return;
        }

        for (Long idTruSo : thongTinDieuPhoi.getDanhSachIdTruSo()) {
            if (!idTruSo.equals(sosDaLuu.getIdTruSoTiepNhan())) {
                Map<String, Object> thongBaoXoa = new HashMap<>();

                thongBaoXoa.put(FIELD_LOAI_THONG_BAO, NOTIFY_DELETE_SOS);
                thongBaoXoa.put(FIELD_ID_SOS_LOWER, sosDaLuu.getId());
                thongBaoXoa.put(FIELD_REASON, REASON_VIP_OVERRIDE);

                messagingTemplate.convertAndSend(
                        TOPIC_TRU_SO_PREFIX + idTruSo + TOPIC_DIEU_PHOI_SUFFIX,
                        thongBaoXoa
                );
            }
        }

        dieuPhoiService.danhDauDaTiepNhan(
                sosDaLuu.getId(),
                sosDaLuu.getIdTruSoTiepNhan()
        );
    }

    private void sendFinalRealtimeMessages(TinHieuSOS sosDaLuu) {
        messagingTemplate.convertAndSend(TOPIC_ADMIN_SOS, sosDaLuu);
        messagingTemplate.convertAndSend(
                TOPIC_USER_PREFIX + sosDaLuu.getUserId() + TOPIC_HISTORY_SUFFIX,
                MSG_REFRESH
        );
    }

    private String saveBase64ToFile(String base64Data, String prefix) {
        try {
            String extension = prefix.contains("audio") ? AUDIO_EXTENSION : IMAGE_EXTENSION;
            String fileName = System.currentTimeMillis() + "_" + prefix + extension;

            Path uploadPath = Paths.get(
                    System.getProperty("user.dir"),
                    UPLOAD_FOLDER,
                    SOS_FOLDER
            );

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String base64Content = base64Data.contains(",")
                    ? base64Data.split(",")[1]
                    : base64Data;

            byte[] bytes = Base64.getDecoder().decode(base64Content);

            Files.write(uploadPath.resolve(fileName), bytes);

            return UPLOAD_URL_PREFIX + fileName;
        } catch (IOException | IllegalArgumentException e) {
            log.error("Lỗi lưu file SOS: {}", e.getMessage());
            return null;
        }
    }
}