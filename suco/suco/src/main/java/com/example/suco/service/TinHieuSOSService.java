package com.example.suco.service;

import com.example.suco.dto.TinHieuSOSRequestDTO;
import com.example.suco.model.MuaGoi;
import com.example.suco.model.TinHieuSOS;
import com.example.suco.model.TruSo;
import com.example.suco.repository.MuaGoiRepository;
import com.example.suco.repository.TinHieuSOSRepository;
import com.example.suco.service.DieuPhoiSOSService.ThongTinDieuPhoi;
import com.example.suco.util.GeocodingUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class TinHieuSOSService {

    private static final Logger log = LoggerFactory.getLogger(TinHieuSOSService.class);

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
        TinHieuSOS sos = new TinHieuSOS();
        sos.setUserId(uid);
        sos.setViDo(dto.getViDo());
        sos.setKinhDo(dto.getKinhDo());
        sos.setGhiChu(dto.getGhiChu());

        // 1. Xử lý Geocoding an toàn (Tránh ConnectException)
       if (dto.getDiaChi() != null && !dto.getDiaChi().isEmpty()) {
        // Nếu App đã tự lấy địa chỉ rồi, dùng luôn, KHÔNG gọi API Geocoding nữa
        sos.setDiaChi(dto.getDiaChi());
    } else {
        // Chỉ gọi khi App không gửi (fallback)
        try {
            var addrMap = geocodingUtil.getAddressFromCoordinates(sos.getViDo(), sos.getKinhDo());
            sos.setDiaChi(geocodingUtil.formatAddress(addrMap));
        } catch (Exception e) {
            sos.setDiaChi("Yêu cầu cứu hộ tại: " + sos.getViDo() + ", " + sos.getKinhDo());
        }
    }

        // 2. Lưu File đính kèm
        if (dto.getHinhAnhBase64() != null && !dto.getHinhAnhBase64().isEmpty()) {
            sos.setHinhAnh(saveBase64ToFile(dto.getHinhAnhBase64(), "sos_img"));
        }
        if (dto.getGhiAmBase64() != null && !dto.getGhiAmBase64().isEmpty()) {
            sos.setGhiAm(saveBase64ToFile(dto.getGhiAmBase64(), "sos_audio"));
        }

        // 3. Khởi tạo trạng thái ban đầu và LƯU ĐỂ LẤY ID (Tránh NullPointerException ở DieuPhoiService)
        sos.setTrangThai("CHO_XU_LY");
        TinHieuSOS sosDaLuu = tinHieuSOSRepository.save(sos);

log.info("=== DEBUG VIP USER {} ===", uid);

List<MuaGoi> listGoi = muaGoiRepository.findByUserId(uid);

boolean laVip = listGoi.stream()
        .anyMatch(mg -> "ACTIVE".equalsIgnoreCase(mg.getTrangThai()));
log.info("VIP STATUS = {}", laVip);

sosDaLuu.setIsVip(laVip);
// 5. Khởi tạo luồng điều phối (Cần ID đã lưu)
        ThongTinDieuPhoi thongTinDieuPhoi = dieuPhoiService.khoiTaoDieuPhoi(sosDaLuu);

        TruSo truSoGanNhat = null;
        if (thongTinDieuPhoi != null) {
            Long idTruSoDauTien = thongTinDieuPhoi.layIdTruSoHienTai();
            if (idTruSoDauTien != null) {
                truSoGanNhat = truSoService.timTruSoTheoId(idTruSoDauTien);
                sosDaLuu.setIdTruSoDeXuat(idTruSoDauTien);
            }
        }


        // 6. Xử lý logic Ưu tiên/Gói cứu hộ
        if (laVip && truSoGanNhat != null) {
            log.info("Phát hiện User {} có gói đặc quyền. Tự động gán trụ sở {}", sosDaLuu.getUserId(), truSoGanNhat.getTenTruSo());
            
            sosDaLuu.setIdTruSoTiepNhan(truSoGanNhat.getId());
            sosDaLuu.setTrangThai("DANG_XU_LY");
            
            // Đồng bộ trạng thái vào bộ nhớ điều phối
            dieuPhoiService.danhDauDaTiepNhan(sosDaLuu.getId(), sosDaLuu.getIdTruSoTiepNhan());

            // Thông báo tức thì cho App User
            Map<String, Object> thongBaoApp = new HashMap<>();
            thongBaoApp.put("idSOS", sosDaLuu.getId());
            thongBaoApp.put("trangThai", "DANG_XU_LY");
            thongBaoApp.put("message", "Bạn có gói đặc quyền! Trụ sở " + truSoGanNhat.getTenTruSo() + " đang đến ngay.");
            messagingTemplate.convertAndSend("/topic/user/" + sosDaLuu.getUserId() + "/sos-status", thongBaoApp);
            
            // Gửi SOS thẳng vào màn hình xử lý của Trụ sở
            messagingTemplate.convertAndSend("/topic/truso/" + sosDaLuu.getIdTruSoTiepNhan(), sosDaLuu);

            // Nếu tín hiệu đã được gửi đến danh sách chờ chung, gửi lệnh xóa cho các trụ sở khác
            if (thongTinDieuPhoi != null && thongTinDieuPhoi.getDanhSachIdTruSo().size() > 1) {
                for (Long idTruSo : thongTinDieuPhoi.getDanhSachIdTruSo()) {
                    if (!idTruSo.equals(sosDaLuu.getIdTruSoTiepNhan())) {
                        Map<String, Object> thongBaoXoa = new HashMap<>();
                        thongBaoXoa.put("loaiThongBao", "XOA_SOS");
                        thongBaoXoa.put("idSos", sosDaLuu.getId());
                        thongBaoXoa.put("reason", "VIP_GHI_DE");
                        messagingTemplate.convertAndSend("/topic/truso/" + idTruSo + "/dieu-phoi", thongBaoXoa);
                    }
                }
                // Dừng điều phối vì đã được VIP tiếp nhận
                dieuPhoiService.danhDauDaTiepNhan(sosDaLuu.getId(), sosDaLuu.getIdTruSoTiepNhan());
            }
        }

        // 7. Lưu lại các thay đổi cuối cùng (ID Trụ sở tiếp nhận/đề xuất)
        sosDaLuu = tinHieuSOSRepository.save(sosDaLuu);

        // 8. Gửi thông báo WebSocket cho các bên liên quan
        messagingTemplate.convertAndSend("/topic/admin/sos", sosDaLuu);
        messagingTemplate.convertAndSend("/topic/user/" + sosDaLuu.getUserId() + "/history", "REFRESH");

        Map<String, Object> ketQua = new HashMap<>();
        ketQua.put("sosData", sosDaLuu);
        ketQua.put("truSoGanNhat", truSoGanNhat);
        ketQua.put("thongTinDieuPhoi", thongTinDieuPhoi);
        return ketQua;
    }

    private String saveBase64ToFile(String base64Data, String prefix) {
        try {
            String extension = prefix.contains("audio") ? ".m4a" : ".jpg";
            String fileName = System.currentTimeMillis() + "_" + prefix + extension;
            
            Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads", "sos");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String base64Content = base64Data.contains(",") ? base64Data.split(",")[1] : base64Data;
            byte[] bytes = Base64.getDecoder().decode(base64Content);
            
            Files.write(uploadPath.resolve(fileName), bytes);
            return "/uploads/sos/" + fileName;
        } catch (IOException e) {
            log.error("Lỗi lưu file SOS: {}", e.getMessage());
            return null;
        }
    }
}