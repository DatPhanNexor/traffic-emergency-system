package com.example.suco.service;

import com.example.suco.model.DoiQua;
import com.example.suco.model.Goi;
import com.example.suco.model.HoaDon;
import com.example.suco.model.MuaGoi;
import com.example.suco.model.Qua;
import com.example.suco.model.TinHieuSOS;
import com.example.suco.model.TruSo;
import com.example.suco.repository.DoiQuaRepository;
import com.example.suco.repository.GoiRepository;
import com.example.suco.repository.HoaDonRepository;
import com.example.suco.repository.MuaGoiRepository;
import com.example.suco.repository.QuaRepository;
import com.example.suco.repository.TinHieuSOSRepository;
import com.example.suco.repository.TruSoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HoaDonService {

    private static final String STATUS_DANG_XU_LY = "DANG_XU_LY";
    private static final String STATUS_HOAN_THANH = "HOAN_THANH";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_PENDING = "PENDING";

    private static final BigDecimal DON_GIA_KM_VUOT_GOI = BigDecimal.valueOf(10000);
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private MuaGoiRepository muaGoiRepository;

    @Autowired
    private GoiRepository goiRepository;

    @Autowired
    private TinHieuSOSRepository tinHieuSOSRepository;

    @Autowired
    private TruSoRepository truSoRepository;

    @Autowired
    private DoiQuaRepository doiQuaRepository;

    @Autowired
    private QuaRepository quaRepository;

    @Transactional
    public HoaDon taoHoaDon(
            Long sosId,
            String tenSos,
            String xuLy,
            Double giaThuCong,
            Long trusoId,
            Long quaId
    ) {
        validateRequiredIds(sosId, trusoId);

        TinHieuSOS sos = tinHieuSOSRepository.findById(sosId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy SOS"));

        TruSo truso = truSoRepository.findById(trusoId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Trụ sở"));

        validateSosCanCreateInvoice(sos, trusoId);

        /*
         * FIX SVP03-BUG-005:
         * Không cho tạo hóa đơn trùng cho cùng một SOS.
         *
         * Riêng case có quaId:
         * Cho phép cập nhật voucher vào hóa đơn đã tồn tại.
         * Đây là case ITC_42.2 trong Postman: tạo/cập nhật hóa đơn có voucher
         * sau khi SOS đã hoàn thành.
         */
        Optional<HoaDon> hoaDonDaCo = hoaDonRepository.findBySosId(sosId);

        if (hoaDonDaCo.isPresent()) {
            HoaDon existing = hoaDonDaCo.get();

            if (quaId != null) {
                apDungVoucherChoHoaDon(existing, quaId);
                return hoaDonRepository.save(existing);
            }

            throw new RuntimeException("SOS này đã có hóa đơn");
        }

        BigDecimal giaGoc = tinhGiaGoc(sos, truso, giaThuCong);
        BigDecimal soTienGiam = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        HoaDon hd = new HoaDon();
        hd.setSosId(sosId);
        hd.setTrusoId(trusoId);
        hd.setUserId(sos.getUserId());
        hd.setTenSos(tenSos);
        hd.setNoiDungXuLy(xuLy);
        hd.setThanhTien(giaGoc);

        if (quaId != null) {
            soTienGiam = tinhTienGiamTuVoucher(giaGoc, quaId);
            hd.setQuaId(quaId);
        }

        hd.setSoTienGiam(soTienGiam);
        hd.setTongThanhToan(tinhTongThanhToan(giaGoc, soTienGiam));
        hd.setTrangThai(
                hd.getTongThanhToan().compareTo(BigDecimal.ZERO) == 0
                        ? STATUS_PAID
                        : STATUS_PENDING
        );

        HoaDon savedHd = hoaDonRepository.save(hd);

        sos.setHoaDon(savedHd);
        tinHieuSOSRepository.save(sos);

        if (quaId != null) {
            truVoucherCuaUser(savedHd.getUserId(), quaId);
        }

        return savedHd;
    }

    @Transactional
    public void apDungVoucherChoHoaDon(HoaDon hd, Long quaId) {
        if (hd == null) {
            throw new RuntimeException("Không tìm thấy hóa đơn");
        }

        if (quaId == null) {
            throw new RuntimeException("Voucher không được để trống");
        }

        if (STATUS_PAID.equalsIgnoreCase(hd.getTrangThai())) {
            throw new RuntimeException("Hóa đơn đã được thanh toán trước đó");
        }

        BigDecimal giaGoc = normalizeMoney(hd.getThanhTien());
        BigDecimal soTienGiam = tinhTienGiamTuVoucher(giaGoc, quaId);

        boolean daApDungVoucherNay =
                hd.getQuaId() != null && hd.getQuaId().equals(quaId);

        hd.setQuaId(quaId);
        hd.setSoTienGiam(soTienGiam);
        hd.setTongThanhToan(tinhTongThanhToan(giaGoc, soTienGiam));
        hd.setTrangThai(
                hd.getTongThanhToan().compareTo(BigDecimal.ZERO) == 0
                        ? STATUS_PAID
                        : STATUS_PENDING
        );

        hoaDonRepository.save(hd);

        /*
         * Tránh trừ voucher lặp lại nếu Postman Runner hoặc frontend gọi lại
         * cùng một hóa đơn với cùng quaId.
         */
        if (!daApDungVoucherNay) {
            truVoucherCuaUser(hd.getUserId(), quaId);
        }
    }

    private void validateRequiredIds(Long sosId, Long trusoId) {
        if (sosId == null) {
            throw new RuntimeException("sosId không được để trống");
        }

        if (trusoId == null) {
            throw new RuntimeException("trusoId không được để trống");
        }
    }

    private void validateSosCanCreateInvoice(TinHieuSOS sos, Long trusoId) {
        if (sos.getIdTruSoTiepNhan() == null) {
            throw new RuntimeException("SOS chưa được tiếp nhận");
        }

        if (!sos.getIdTruSoTiepNhan().equals(trusoId)) {
            throw new RuntimeException("SOS này không thuộc trụ sở của bạn");
        }

        /*
         * FIX SVP03-BUG-004:
         * Trước đây chỉ cho tạo hóa đơn khi SOS đang DANG_XU_LY.
         * Nhưng Postman ITC_42.2 tạo hóa đơn có voucher sau khi SOS đã HOAN_THANH.
         * Vì vậy phải cho phép cả DANG_XU_LY và HOAN_THANH.
         */
        if (!(STATUS_DANG_XU_LY.equals(sos.getTrangThai())
                || STATUS_HOAN_THANH.equals(sos.getTrangThai()))) {
            throw new RuntimeException("Chỉ tạo hóa đơn khi SOS đang xử lý hoặc đã hoàn thành");
        }
    }

    private BigDecimal tinhGiaGoc(TinHieuSOS sos, TruSo truso, Double giaThuCong) {
        Optional<MuaGoi> muaGoiOpt =
                muaGoiRepository.findFirstByUserIdAndTrangThai(
                        sos.getUserId(),
                        STATUS_ACTIVE
                );

        if (muaGoiOpt.isEmpty()) {
            return normalizeMoney(
                    BigDecimal.valueOf(giaThuCong != null ? giaThuCong : 0)
            );
        }

        MuaGoi muaGoi = muaGoiOpt.get();

        Goi goi = goiRepository.findById(muaGoi.getGoiId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gói"));

        double distance = calculateDistance(
                truso.getViDo(),
                truso.getKinhDo(),
                sos.getViDo(),
                sos.getKinhDo()
        );

        double freeKm = goi.getKhoangCachMienPhi() != null
                ? goi.getKhoangCachMienPhi()
                : 0;

        double extraKm = Math.max(0, distance - freeKm);
        long soKmTinhTien = (long) Math.ceil(extraKm);

        BigDecimal giaGoc = BigDecimal.valueOf(soKmTinhTien)
                .multiply(DON_GIA_KM_VUOT_GOI);

        return normalizeMoney(giaGoc);
    }

    private BigDecimal tinhTienGiamTuVoucher(BigDecimal giaGoc, Long quaId) {
        Qua voucher = quaRepository.findById(quaId)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));

        if (voucher.getLoai() != Qua.LoaiQua.VOUCHER) {
            throw new RuntimeException("Vật phẩm này không phải là Voucher");
        }

        BigDecimal phanTramGiam = BigDecimal.valueOf(voucher.getGiaTriGiamPercent())
                .divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP);

        BigDecimal soTienGiam = giaGoc.multiply(phanTramGiam);

        if (voucher.getGiaTriToiDa() != null
                && soTienGiam.compareTo(voucher.getGiaTriToiDa()) > 0) {
            soTienGiam = voucher.getGiaTriToiDa();
        }

        if (soTienGiam.compareTo(BigDecimal.ZERO) < 0) {
            soTienGiam = BigDecimal.ZERO;
        }

        if (soTienGiam.compareTo(giaGoc) > 0) {
            soTienGiam = giaGoc;
        }

        return normalizeMoney(soTienGiam);
    }

    private BigDecimal tinhTongThanhToan(BigDecimal giaGoc, BigDecimal soTienGiam) {
        BigDecimal tongThanhToan = normalizeMoney(giaGoc)
                .subtract(normalizeMoney(soTienGiam));

        if (tongThanhToan.compareTo(BigDecimal.ZERO) < 0) {
            tongThanhToan = BigDecimal.ZERO;
        }

        return normalizeMoney(tongThanhToan);
    }

    private void truVoucherCuaUser(String userId, Long quaId) {
        if (userId == null || quaId == null) {
            return;
        }

        Optional<DoiQua> doiQuaOpt = doiQuaRepository.findByUserIdAndQuaId(userId, quaId);

        if (doiQuaOpt.isEmpty()) {
            return;
        }

        DoiQua doiQua = doiQuaOpt.get();

        if (doiQua.getSoLuong() != null && doiQua.getSoLuong() > 1) {
            doiQua.setSoLuong(doiQua.getSoLuong() - 1);
            doiQuaRepository.save(doiQua);
            return;
        }

        doiQuaRepository.delete(doiQua);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);

        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}