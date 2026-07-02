package com.example.suco.repository;

import com.example.suco.model.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoaDonRepository extends JpaRepository<HoaDon, Long> {

    /*
     * Dùng cho SVP03-BUG-004 / SVP03-BUG-005:
     * - Kiểm tra SOS đã có hóa đơn hay chưa.
     * - Nếu đã có hóa đơn và request có voucher thì cập nhật voucher vào hóa đơn cũ.
     * - Nếu đã có hóa đơn và request không có voucher thì chặn tạo trùng.
     */
    Optional<HoaDon> findBySosId(Long sosId);

    /*
     * Dự phòng khi một SOS lỡ phát sinh nhiều hóa đơn trong dữ liệu cũ.
     * Lấy hóa đơn mới nhất theo id.
     */
    Optional<HoaDon> findFirstBySosIdOrderByIdDesc(Long sosId);

    /*
     * Lấy danh sách hóa đơn của một trụ sở, hóa đơn mới nhất lên trước.
     * Dùng cho API GET /api/hoa-don/danh-sach.
     */
    List<HoaDon> findByTrusoIdOrderByIdDesc(Long trusoId);

    /*
     * Lọc hóa đơn theo trụ sở và trạng thái.
     * Ví dụ:
     * - PENDING: hóa đơn chưa thanh toán
     * - PAID: hóa đơn đã thanh toán
     */
    List<HoaDon> findByTrusoIdAndTrangThai(Long trusoId, String trangThai);
}