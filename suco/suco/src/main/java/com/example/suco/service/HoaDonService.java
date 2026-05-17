package com.example.suco.service;

import com.example.suco.model.*;
import com.example.suco.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HoaDonService {
    @Autowired private HoaDonRepository hoaDonRepository;
    @Autowired private MuaGoiRepository muaGoiRepository;
    @Autowired private GoiRepository goiRepository;
    @Autowired private TinHieuSOSRepository tinHieuSOSRepository;
    @Autowired private TruSoRepository truSoRepository;
    @Autowired private DoiQuaRepository doiQuaRepository;
    @Autowired private QuaRepository quaRepository;

    @Transactional // Quan trọng: Đảm bảo tính toàn vẹn dữ liệu
    public HoaDon taoHoaDon(Long sosId, String tenSos, String xuLy, Double giaThuCong, Long trusoId, Long quaId) {
        
        // 1. Tìm thông tin cơ bản
        TinHieuSOS sos = tinHieuSOSRepository.findById(sosId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy SOS"));
        TruSo truso = truSoRepository.findById(trusoId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy Trụ sở"));

  // ✅ chưa có trụ sở nhận
if (sos.getIdTruSoTiepNhan() == null) {
    throw new RuntimeException("SOS chưa được tiếp nhận");
}

// ✅ sai trụ sở
if (!sos.getIdTruSoTiepNhan().equals(trusoId)) {
    throw new RuntimeException("SOS này không thuộc trụ sở của bạn");
}

// ✅ sai trạng thái
if (!"DANG_XU_LY".equals(sos.getTrangThai())) {
    throw new RuntimeException("Chỉ tạo hóa đơn khi SOS đang xử lý");
}
        HoaDon hd = new HoaDon();
        hd.setSosId(sosId);
        hd.setTrusoId(trusoId);
        hd.setUserId(sos.getUserId());
        hd.setTenSos(tenSos);
        hd.setNoiDungXuLy(xuLy);

        // 2. Tính GIÁ GỐC (thanhTien)
        BigDecimal giaGoc = BigDecimal.ZERO;
        Optional<MuaGoi> muaGoiOpt = muaGoiRepository.findFirstByUserIdAndTrangThai(sos.getUserId(), "ACTIVE");

        if (muaGoiOpt.isPresent()) {
    Goi goi = goiRepository.findById(muaGoiOpt.get().getGoiId()).get();
    double distance = calculateDistance(truso.getViDo(), truso.getKinhDo(), sos.getViDo(), sos.getKinhDo());
    double freeKm = (goi.getKhoangCachMienPhi() != null) ? goi.getKhoangCachMienPhi() : 0;

    // ❌ XÓA dòng này
    // double finalPrice = (distance > freeKm) ? (distance - freeKm) * 10000 : 0;

    // ✅ THAY BẰNG
    double extraKm = Math.max(0, distance - freeKm);
    long soKmTinhTien = (long) Math.ceil(extraKm);
    double finalPrice = soKmTinhTien * 10000;

    giaGoc = BigDecimal.valueOf(finalPrice);
} else {
            giaGoc = BigDecimal.valueOf(giaThuCong != null ? giaThuCong : 0);
        }
        hd.setThanhTien(giaGoc);

        // 3. Tính GIẢM GIÁ
        BigDecimal soTienGiam = BigDecimal.ZERO;
        if (quaId != null) {
            Optional<Qua> quaOpt = quaRepository.findById(quaId);
            if (quaOpt.isPresent() && quaOpt.get().getLoai() == Qua.LoaiQua.VOUCHER) {
                Qua v = quaOpt.get();
                hd.setQuaId(quaId);

                // Tiền giảm = Giá gốc * % / 100
                BigDecimal phanTram = BigDecimal.valueOf(v.getGiaTriGiamPercent()).divide(new BigDecimal(100));
                soTienGiam = giaGoc.multiply(phanTram);

                // Kiểm tra trần giảm giá
                if (v.getGiaTriToiDa() != null && soTienGiam.compareTo(v.getGiaTriToiDa()) > 0) {
                    soTienGiam = v.getGiaTriToiDa();
                }
            }
        }
        hd.setSoTienGiam(soTienGiam);

        // 4. Tính TỔNG THANH TOÁN & THIẾT LẬP TRẠNG THÁI
        BigDecimal thucTra = giaGoc.subtract(soTienGiam);
        if (thucTra.compareTo(BigDecimal.ZERO) < 0) thucTra = BigDecimal.ZERO;
        
        hd.setTongThanhToan(thucTra);
        
        // Cập nhật trạng thái trước khi save
        hd.setTrangThai(thucTra.compareTo(BigDecimal.ZERO) == 0 ? "PAID" : "PENDING");

        // 5. Lưu hóa đơn
        HoaDon savedHd = hoaDonRepository.save(hd);
        // Cập nhật lại SOS với ID hóa đơn mới tạo
        sos.setHoaDon(savedHd); 
        tinHieuSOSRepository.save(sos);

        // 6. Xử lý XÓA hoặc TRỪ Voucher (Sau khi save HD thành công)
        if (quaId != null) {
            doiQuaRepository.findByUserIdAndQuaId(savedHd.getUserId(), quaId).ifPresent(dq -> {
                if (dq.getSoLuong() != null && dq.getSoLuong() > 1) {
                    dq.setSoLuong(dq.getSoLuong() - 1);
                    doiQuaRepository.save(dq);
                } else {
                    doiQuaRepository.delete(dq);
                }
                System.out.println("===> Đã xử lý Voucher cho User: " + savedHd.getUserId());
            });
        }

        return savedHd; // Trả về kết quả cuối cùng ở đây
    }
    @Transactional
public void apDungVoucherChoHoaDon(HoaDon hd, Long quaId) {
    // 1. Lấy thông tin Voucher từ bảng 'qua'
    Qua voucher = quaRepository.findById(quaId)
            .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));

    if (voucher.getLoai() != Qua.LoaiQua.VOUCHER) {
        throw new RuntimeException("Vật phẩm này không phải là Voucher");
    }

    // 2. Tính toán lại số tiền giảm
    BigDecimal giaGoc = hd.getThanhTien();
    BigDecimal phanTram = BigDecimal.valueOf(voucher.getGiaTriGiamPercent())
            .divide(new BigDecimal(100));
    BigDecimal soTienGiam = giaGoc.multiply(phanTram);

    // Kiểm tra trần giảm giá (nếu có)
    if (voucher.getGiaTriToiDa() != null && soTienGiam.compareTo(voucher.getGiaTriToiDa()) > 0) {
        soTienGiam = voucher.getGiaTriToiDa();
    }

    // 3. Cập nhật thông tin vào hóa đơn
    hd.setQuaId(quaId);
    hd.setSoTienGiam(soTienGiam);
    
    BigDecimal tongMoi = giaGoc.subtract(soTienGiam);
    if (tongMoi.compareTo(BigDecimal.ZERO) < 0) tongMoi = BigDecimal.ZERO;
    hd.setTongThanhToan(tongMoi);

    // 4. Thực hiện trừ số lượng Voucher trong túi của người dùng
    doiQuaRepository.findByUserIdAndQuaId(hd.getUserId(), quaId).ifPresent(dq -> {
        if (dq.getSoLuong() != null && dq.getSoLuong() > 1) {
            dq.setSoLuong(dq.getSoLuong() - 1);
            doiQuaRepository.save(dq);
        } else {
            doiQuaRepository.delete(dq);
        }
    });
    
    // Lưu lại hóa đơn đã cập nhật số tiền
    hoaDonRepository.save(hd);
}
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // KM
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}