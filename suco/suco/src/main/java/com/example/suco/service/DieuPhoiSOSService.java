package com.example.suco.service;

import com.example.suco.model.TinHieuSOS;
import com.example.suco.model.TruSo;
import com.example.suco.repository.TinHieuSOSRepository;
import com.example.suco.repository.TruSoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import ch.hsr.geohash.GeoHash;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Dịch vụ điều phối SOS tự động - LƯU TRONG BỘ NHỚ (MEMORY).
 * Tìm các trụ sở trong vùng Geohash, sắp xếp theo khoảng cách,
 * và tự động chuyển tiếp sau 60 giây nếu không được tiếp nhận.
 */
@Service
public class DieuPhoiSOSService {

    private static final Logger nhatKy = LoggerFactory.getLogger(DieuPhoiSOSService.class);

    // Thời gian chờ trước khi chuyển tiếp (giây)
    public static final int THOI_GIAN_CHO_TIEP_NHAN = 60;

    // ==================== LƯU TRỮ TRONG BỘ NHỚ ====================
    // Map lưu trữ thông tin điều phối: Key = idSOS, Value = ThongTinDieuPhoi
    private final ConcurrentHashMap<Long, ThongTinDieuPhoi> bangDieuPhoi = new ConcurrentHashMap<>();

    @Autowired
    private SimpMessagingTemplate mauGuiTinNhan;

    @Autowired
    private TinHieuSOSRepository tinHieuSOSRepository;

    @Autowired
    private TruSoRepository truSoRepository;

    /**
     * Class lưu thông tin điều phối trong bộ nhớ.
     */
    public static class ThongTinDieuPhoi {
        private Long idSos;
        private List<Long> danhSachIdTruSo;       // Danh sách ID trụ sở từ gần đến xa
        private List<Long> danhSachKhoangCach;    // Khoảng cách tương ứng (mét)
        private int chiMucTruSoHienTai;           // Vị trí hiện tại (0-based)
        private LocalDateTime thoiGianGuiTinCuoi; // Mốc thời gian để tính 60 giây
        private String trangThaiDieuPhoi;         // DANG_CHO, DA_TIEP_NHAN, HET_TRU_SO, HUY_BO

        public ThongTinDieuPhoi(Long idSos, List<Long> danhSachIdTruSo, List<Long> danhSachKhoangCach) {
            this.idSos = idSos;
            this.danhSachIdTruSo = danhSachIdTruSo;
            this.danhSachKhoangCach = danhSachKhoangCach;
            this.chiMucTruSoHienTai = 0;
            this.thoiGianGuiTinCuoi = LocalDateTime.now();
            this.trangThaiDieuPhoi = "DANG_CHO";
        }

        // Getters & Setters
        public Long getIdSos() { return idSos; }
        public List<Long> getDanhSachIdTruSo() { return danhSachIdTruSo; }
        public List<Long> getDanhSachKhoangCach() { return danhSachKhoangCach; }
        public int getChiMucTruSoHienTai() { return chiMucTruSoHienTai; }
        public void setChiMucTruSoHienTai(int chiMucTruSoHienTai) { this.chiMucTruSoHienTai = chiMucTruSoHienTai; }
        public LocalDateTime getThoiGianGuiTinCuoi() { return thoiGianGuiTinCuoi; }
        public void setThoiGianGuiTinCuoi(LocalDateTime thoiGianGuiTinCuoi) { this.thoiGianGuiTinCuoi = thoiGianGuiTinCuoi; }
        public String getTrangThaiDieuPhoi() { return trangThaiDieuPhoi; }
        public void setTrangThaiDieuPhoi(String trangThaiDieuPhoi) { this.trangThaiDieuPhoi = trangThaiDieuPhoi; }

        // Helper methods
        public Long layIdTruSoHienTai() {
            if (chiMucTruSoHienTai < danhSachIdTruSo.size()) {
                return danhSachIdTruSo.get(chiMucTruSoHienTai);
            }
            return null;
        }

        public boolean conTruSoTiepTheo() {
            return (chiMucTruSoHienTai + 1) < danhSachIdTruSo.size();
        }

        public Long layIdTruSoTiepTheo() {
            if (!conTruSoTiepTheo()) return null;
            return danhSachIdTruSo.get(chiMucTruSoHienTai + 1);
        }

        public int getTongSoTruSo() {
            return danhSachIdTruSo.size();
        }
    }

    /**
     * Khởi tạo quy trình điều phối khi có SOS mới.
     * Tìm tất cả trụ sở trong vùng, sắp xếp theo khoảng cách, và gửi cho trụ sở gần nhất.
     */
    public ThongTinDieuPhoi khoiTaoDieuPhoi(TinHieuSOS tinHieuSos) {
        nhatKy.info("==================== BẮT ĐẦU ĐIỀU PHỐI SOS #{} ====================", tinHieuSos.getId());

        Double viDoNguoiGui = tinHieuSos.getViDo();
        Double kinhDoNguoiGui = tinHieuSos.getKinhDo();

        // 1. Tìm tất cả trụ sở trong vùng Geohash (mở rộng dần nếu không có)
        List<TruSo> danhSachTruSo = timTatCaTruSoTrongVung(viDoNguoiGui, kinhDoNguoiGui);

        if (danhSachTruSo.isEmpty()) {
            nhatKy.warn("[CẢNH BÁO] Không tìm thấy trụ sở nào để điều phối SOS #{}", tinHieuSos.getId());
            return null;
        }

        // 2. Tính khoảng cách và sắp xếp từ gần đến xa
        List<TruSoVoiKhoangCach> danhSachDaSapXep = new ArrayList<>();
        for (TruSo truSo : danhSachTruSo) {
            double khoangCachMet = tinhKhoangCachMet(viDoNguoiGui, kinhDoNguoiGui, truSo.getViDo(), truSo.getKinhDo());
            danhSachDaSapXep.add(new TruSoVoiKhoangCach(truSo, khoangCachMet));
        }

        // Sắp xếp theo khoảng cách tăng dần
        danhSachDaSapXep.sort(Comparator.comparingDouble(ts -> ts.khoangCachMet));

        // Log danh sách đã sắp xếp
        nhatKy.info("[DANH SÁCH TRỤ SỞ THEO KHOẢNG CÁCH]");
        for (int i = 0; i < danhSachDaSapXep.size(); i++) {
            TruSoVoiKhoangCach ts = danhSachDaSapXep.get(i);
            nhatKy.info("   {}. {} (ID={}) - {} mét", i + 1, ts.truSo.getTenTruSo(), ts.truSo.getId(), Math.round(ts.khoangCachMet));
        }

        // 3. Tạo danh sách ID và khoảng cách
        List<Long> danhSachIdTruSo = danhSachDaSapXep.stream()
                .map(ts -> ts.truSo.getId())
                .collect(Collectors.toList());

        List<Long> danhSachKhoangCach = danhSachDaSapXep.stream()
                .map(ts -> Math.round(ts.khoangCachMet))
                .collect(Collectors.toList());

        // 4. Tạo thông tin điều phối và lưu vào bộ nhớ
        ThongTinDieuPhoi thongTinDieuPhoi = new ThongTinDieuPhoi(tinHieuSos.getId(), danhSachIdTruSo, danhSachKhoangCach);
        bangDieuPhoi.put(tinHieuSos.getId(), thongTinDieuPhoi);

        // 5. Cập nhật trụ sở đề xuất cho SOS
        Long idTruSoDauTien = danhSachIdTruSo.get(0);
        tinHieuSos.setIdTruSoDeXuat(idTruSoDauTien);
        tinHieuSOSRepository.save(tinHieuSos);

        // 6. Gửi thông báo cho trụ sở đầu tiên (gần nhất)
        guiThongBaoChoTruSo(idTruSoDauTien, tinHieuSos, 0, danhSachIdTruSo.size());

        nhatKy.info("[KẾT QUẢ] Đã gửi SOS #{} đến trụ sở {} ({})",
            tinHieuSos.getId(),
            danhSachDaSapXep.get(0).truSo.getTenTruSo(),
            Math.round(danhSachDaSapXep.get(0).khoangCachMet) + " mét");
        nhatKy.info("================================================================");

        return thongTinDieuPhoi;
    }

    /**
     * Chuyển tiếp SOS sang trụ sở tiếp theo khi hết thời gian chờ.
     */
    public boolean chuyenTiepSangTruSoTiepTheo(ThongTinDieuPhoi thongTinDieuPhoi) {
        Long idSos = thongTinDieuPhoi.getIdSos();
        Long idTruSoCu = thongTinDieuPhoi.layIdTruSoHienTai();

        if (!thongTinDieuPhoi.conTruSoTiepTheo()) {
            // Không còn trụ sở nào để chuyển tiếp
            thongTinDieuPhoi.setTrangThaiDieuPhoi("HET_TRU_SO");
            nhatKy.warn("[HẾT TRỤ SỞ] SOS #{} đã hết danh sách trụ sở có thể tiếp nhận", idSos);
            return false;
        }

        // Lấy trụ sở tiếp theo
        int chiMucMoi = thongTinDieuPhoi.getChiMucTruSoHienTai() + 1;
        Long idTruSoMoi = thongTinDieuPhoi.layIdTruSoTiepTheo();

        // Cập nhật thông tin điều phối trong bộ nhớ
        thongTinDieuPhoi.setChiMucTruSoHienTai(chiMucMoi);
        thongTinDieuPhoi.setThoiGianGuiTinCuoi(LocalDateTime.now());

        // Lấy thông tin SOS
        Optional<TinHieuSOS> optSos = tinHieuSOSRepository.findById(idSos);
        if (optSos.isEmpty()) {
            nhatKy.error("[LỖI] Không tìm thấy SOS #{} để chuyển tiếp", idSos);
            return false;
        }

        TinHieuSOS tinHieuSos = optSos.get();

        // Cập nhật trụ sở đề xuất mới
        tinHieuSos.setIdTruSoDeXuat(idTruSoMoi);
        tinHieuSOSRepository.save(tinHieuSos);

        // Lấy thông tin trụ sở để log
        String tenTruSoCu = truSoRepository.findById(idTruSoCu).map(TruSo::getTenTruSo).orElse("Không xác định");
        String tenTruSoMoi = truSoRepository.findById(idTruSoMoi).map(TruSo::getTenTruSo).orElse("Không xác định");
        int tongSoTruSo = thongTinDieuPhoi.getTongSoTruSo();

        nhatKy.info("[CHUYỂN TIẾP] SOS #{}: {} -> {} (Vị trí {}/{})",
            idSos, tenTruSoCu, tenTruSoMoi, chiMucMoi + 1, tongSoTruSo);

        // 1. Gửi lệnh xóa SOS ở trụ sở cũ (theo định dạng mà client xử lý trên kênh /dieu-phoi)
        Map<String, Object> thongBaoXoa = new HashMap<>();
        thongBaoXoa.put("loaiThongBao", "XOA_SOS");
        thongBaoXoa.put("idSos", idSos);
        thongBaoXoa.put("reason", "CHUYEN_TIEP_QUA_HAN");
        mauGuiTinNhan.convertAndSend("/topic/truso/" + idTruSoCu + "/dieu-phoi", thongBaoXoa);

        // 2. Gửi SOS đến trụ sở mới
        guiThongBaoChoTruSo(idTruSoMoi, tinHieuSos, chiMucMoi, tongSoTruSo);

        return true;
    }

    /**
     * Đánh dấu SOS đã được tiếp nhận, dừng quy trình điều phối.
     */
    public void danhDauDaTiepNhan(Long idSos, Long idTruSoTiepNhan) {
        ThongTinDieuPhoi thongTin = bangDieuPhoi.get(idSos);
        if (thongTin != null) {
            thongTin.setTrangThaiDieuPhoi("DA_TIEP_NHAN");

            nhatKy.info("[TIẾP NHẬN] SOS #{} đã được trụ sở ID={} tiếp nhận", idSos, idTruSoTiepNhan);

            // Gửi thông báo xóa cho các trụ sở khác trong danh sách
            List<Long> danhSachId = thongTin.getDanhSachIdTruSo();
            for (Long idTruSo : danhSachId) {
                if (!idTruSo.equals(idTruSoTiepNhan)) {
                    Map<String, Object> thongBaoXoa = new HashMap<>();
                    thongBaoXoa.put("loaiThongBao", "XOA_SOS");
                    thongBaoXoa.put("idSos", idSos);
                    thongBaoXoa.put("reason", "DA_DUOC_TIEP_NHAN");
                    mauGuiTinNhan.convertAndSend("/topic/truso/" + idTruSo + "/dieu-phoi", thongBaoXoa);
                }
            }

            // Xóa khỏi bộ nhớ sau khi tiếp nhận
            bangDieuPhoi.remove(idSos);
        }
    }

    /**
     * Từ chối tiếp nhận SOS - chuyển tiếp ngay lập tức cho trụ sở tiếp theo.
     * Trả về true nếu còn trụ sở để chuyển tiếp, false nếu hết.
     */
    public boolean tuChoiTiepNhan(Long idSos, Long idTruSoTuChoi) {
        ThongTinDieuPhoi thongTin = bangDieuPhoi.get(idSos);
        if (thongTin == null) {
            nhatKy.warn("[TỪ CHỐI] Không tìm thấy thông tin điều phối cho SOS #{}", idSos);
            return false;
        }

        // Kiểm tra xem trụ sở từ chối có phải là trụ sở đang được gửi tín hiệu không
        Long idTruSoHienTai = thongTin.layIdTruSoHienTai();
        if (!idTruSoTuChoi.equals(idTruSoHienTai)) {
            nhatKy.warn("[TỪ CHỐI] Trụ sở {} không phải là trụ sở đang nhận SOS #{}", idTruSoTuChoi, idSos);
            return false;
        }

        String tenTruSoTuChoi = truSoRepository.findById(idTruSoTuChoi)
                .map(TruSo::getTenTruSo).orElse("Không xác định");
        nhatKy.info("[TỪ CHỐI] Trụ sở {} từ chối tiếp nhận SOS #{}", tenTruSoTuChoi, idSos);

        // Chuyển tiếp sang trụ sở tiếp theo
        return chuyenTiepSangTruSoTiepTheo(thongTin);
    }

    /**
     * Hủy điều phối khi SOS bị hủy.
     */
    public void huyDieuPhoi(Long idSos) {
        ThongTinDieuPhoi thongTin = bangDieuPhoi.get(idSos);
        if (thongTin != null) {
            thongTin.setTrangThaiDieuPhoi("HUY_BO");

            // Gửi thông báo xóa cho tất cả trụ sở trong danh sách
            List<Long> danhSachId = thongTin.getDanhSachIdTruSo();
            for (Long idTruSo : danhSachId) {
                Map<String, Object> thongBaoXoa = new HashMap<>();
                thongBaoXoa.put("loaiThongBao", "XOA_SOS");
                thongBaoXoa.put("idSos", idSos);
                thongBaoXoa.put("reason", "HUY_BO");
                mauGuiTinNhan.convertAndSend("/topic/truso/" + idTruSo + "/dieu-phoi", thongBaoXoa);
            }

            // Xóa khỏi bộ nhớ
            bangDieuPhoi.remove(idSos);
            nhatKy.info("[HỦY] Đã hủy điều phối SOS #{}", idSos);
        }
    }

    /**
     * Lấy danh sách điều phối đã quá thời gian chờ.
     */
    public List<ThongTinDieuPhoi> layDanhSachQuaHan(int soGiayChoPhep) {
        LocalDateTime thoiGianGioiHan = LocalDateTime.now().minusSeconds(soGiayChoPhep);

        return bangDieuPhoi.values().stream()
                .filter(dp -> "DANG_CHO".equals(dp.getTrangThaiDieuPhoi()))
                .filter(dp -> dp.getThoiGianGuiTinCuoi().isBefore(thoiGianGioiHan))
                .collect(Collectors.toList());
    }

    /**
     * Lấy thông tin điều phối theo ID SOS.
     */
    public Optional<ThongTinDieuPhoi> layThongTinDieuPhoi(Long idSos) {
        return Optional.ofNullable(bangDieuPhoi.get(idSos));
    }

    /**
     * Lấy tất cả thông tin điều phối đang hoạt động (để debug).
     */
    public Collection<ThongTinDieuPhoi> layTatCaDieuPhoiDangHoatDong() {
        return bangDieuPhoi.values().stream()
                .filter(dp -> "DANG_CHO".equals(dp.getTrangThaiDieuPhoi()))
                .collect(Collectors.toList());
    }

    // ===== PRIVATE METHODS =====

    /**
     * Tìm tất cả trụ sở trong vùng Geohash (mở rộng dần nếu cần).
     */
    private List<TruSo> timTatCaTruSoTrongVung(double viDo, double kinhDo) {
        List<TruSo> danhSachUngVien = new ArrayList<>();
        int doChinhXac = 6;

        while (doChinhXac >= 4 && danhSachUngVien.isEmpty()) {
            GeoHash hashTrungTam = GeoHash.withCharacterPrecision(viDo, kinhDo, doChinhXac);
            String maHashHienTai = hashTrungTam.toBase32();

            List<String> danhSachTienTo = new ArrayList<>();
            danhSachTienTo.add(maHashHienTai);
            for (GeoHash hashKeLan : hashTrungTam.getAdjacent()) {
                danhSachTienTo.add(hashKeLan.toBase32());
            }

            nhatKy.debug("[TÌM KIẾM] Cấp độ {} | Mã Geohash: {}...", doChinhXac, maHashHienTai);

            if (doChinhXac == 6) {
                danhSachUngVien = truSoRepository.findByGeohashIn(danhSachTienTo);
            } else {
                for (String tienTo : danhSachTienTo) {
                    danhSachUngVien.addAll(truSoRepository.findByGeohashStartingWith(tienTo));
                }
                danhSachUngVien = new ArrayList<>(new HashSet<>(danhSachUngVien));
            }

            if (danhSachUngVien.isEmpty()) {
                nhatKy.debug("   => Không có trụ sở ở cấp {}. Đang lùi cấp...", doChinhXac);
                doChinhXac--;
            }
        }

        // Nếu vẫn không tìm thấy, lấy tất cả
        if (danhSachUngVien.isEmpty()) {
            nhatKy.warn("[CẢNH BÁO] Không tìm thấy trụ sở trong vùng, lấy toàn bộ DB");
            danhSachUngVien = truSoRepository.findAll();
        }

        return danhSachUngVien;
    }

    /**
     * Gửi thông báo SOS đến trụ sở cụ thể.
     */
    private void guiThongBaoChoTruSo(Long idTruSo, TinHieuSOS tinHieuSos, int viTriHienTai, int tongSoTruSo) {
        // Gửi đến kênh chính của trụ sở
        mauGuiTinNhan.convertAndSend("/topic/truso/" + idTruSo, tinHieuSos);

        // Gửi thông tin điều phối bổ sung
        Map<String, Object> thongTinDieuPhoi = new HashMap<>();
        thongTinDieuPhoi.put("loaiThongBao", "SOS_MOI");
        thongTinDieuPhoi.put("idSos", tinHieuSos.getId());
        thongTinDieuPhoi.put("viTriTrongDanhSach", viTriHienTai + 1);
        thongTinDieuPhoi.put("tongSoTruSo", tongSoTruSo);
        thongTinDieuPhoi.put("thoiGianConLai", THOI_GIAN_CHO_TIEP_NHAN);
        mauGuiTinNhan.convertAndSend("/topic/truso/" + idTruSo + "/dieu-phoi", thongTinDieuPhoi);
    }

    /**
     * Tính khoảng cách giữa 2 tọa độ bằng công thức Haversine (đơn vị: mét).
     */
    private double tinhKhoangCachMet(double viDo1, double kinhDo1, double viDo2, double kinhDo2) {
        double banKinhTraiDat = 6371e3; // mét
        double phi1 = Math.toRadians(viDo1);
        double phi2 = Math.toRadians(viDo2);
        double deltaPhi = Math.toRadians(viDo2 - viDo1);
        double deltaLambda = Math.toRadians(kinhDo2 - kinhDo1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                   Math.cos(phi1) * Math.cos(phi2) *
                   Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return banKinhTraiDat * c;
    }

    // ===== INNER CLASS =====

    /**
     * Class helper để lưu trụ sở kèm khoảng cách.
     */
    private static class TruSoVoiKhoangCach {
        TruSo truSo;
        double khoangCachMet;

        TruSoVoiKhoangCach(TruSo truSo, double khoangCachMet) {
            this.truSo = truSo;
            this.khoangCachMet = khoangCachMet;
        }
    }
}
