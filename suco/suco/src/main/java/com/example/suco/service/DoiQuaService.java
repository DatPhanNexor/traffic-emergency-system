package com.example.suco.service;

import com.example.suco.dto.DoiQuaDto;
import com.example.suco.model.*;
import com.example.suco.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Service
public class DoiQuaService {
    @Autowired private DoiQuaRepository doiQuaRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private QuaRepository quaRepository;

@Transactional
public boolean thucHienDoiQua(String uid, DoiQuaDto dto) {

    LocalDateTime now = LocalDateTime.now();

    User user = userRepository.findById(uid)
            .orElseThrow(() -> new RuntimeException("User không tồn tại"));

    Qua qua = quaRepository.findById(dto.getQuaId())
            .orElseThrow(() -> new RuntimeException("Quà không tồn tại"));

    // 1. Check trạng thái
    if (qua.getTrangThai() != Qua.TrangThai.HOAT_DONG) {
        throw new RuntimeException("Quà không còn khả dụng");
    }

    // 2. Check hết hạn
    if (qua.getNgayKetThuc() != null && now.isAfter(qua.getNgayKetThuc())) {
        throw new RuntimeException("Quà đã hết thời gian");
    }

    // 3. Check điểm
    if (user.getTotalPoints() < qua.getDiem()) {
        throw new RuntimeException("Không đủ điểm");
    }

    // 4. Trừ điểm
    user.setTotalPoints(user.getTotalPoints() - qua.getDiem());
    userRepository.save(user);

    // 5. GỘP QUÀ
    Optional<DoiQua> existing =
            doiQuaRepository.findByUserIdAndQuaId(uid, dto.getQuaId());

    if (existing.isPresent()) {
        DoiQua item = existing.get();
        item.setSoLuong(item.getSoLuong() + 1);
        doiQuaRepository.save(item);

    } else {
        DoiQua newItem = new DoiQua();
        newItem.setUserId(uid);
        newItem.setQuaId(dto.getQuaId());
        newItem.setSoLuong(1);

        doiQuaRepository.save(newItem);
    }

    return true;
}
public List<DoiQuaDto> getMyGifts(String uid) {

    return doiQuaRepository.getMyGiftsWithQua(uid)
        .stream()
        .map(obj -> {
            DoiQua d = (DoiQua) obj[0];
            Qua q = (Qua) obj[1];

            DoiQuaDto dto = new DoiQuaDto();
            dto.setQuaId(q.getId());
            dto.setTenQua(q.getTen());
            dto.setLoai(q.getLoai().name());
            dto.setSoLuong(d.getSoLuong());
            dto.setGiaTriGiamPercent(q.getGiaTriGiamPercent());
            dto.setGiaTriToiDa(q.getGiaTriToiDa());
            dto.setNgayKetThuc(q.getNgayKetThuc());

            return dto;
        })
        .toList();
}
}