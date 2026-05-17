package com.example.suco.repository;

import com.example.suco.model.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HoaDonRepository extends JpaRepository<HoaDon, Long> {
    Optional<HoaDon> findBySosId(Long sosId);
    Optional<HoaDon> findFirstBySosIdOrderByIdDesc(Long sosId);
    // Thêm dòng này để tìm hóa đơn theo Trụ sở và sắp xếp mới nhất lên trước
    List<HoaDon> findByTrusoIdOrderByIdDesc(Long trusoId);
    
    // Nếu bạn muốn lọc theo trạng thái (ví dụ chỉ xem các hóa đơn chưa thanh toán)
    List<HoaDon> findByTrusoIdAndTrangThai(Long trusoId, String trangThai);
    
}