package com.example.suco.service;

import com.example.suco.dto.DoiQuaDto;
import com.example.suco.model.*;
import com.example.suco.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

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
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quà không còn khả dụng");
    }

    // 2. Check hết hạn
    if (qua.getNgayKetThuc() != null && now.isAfter(qua.getNgayKetThuc())) {
        throw new RuntimeException("Quà đã hết thời gian");
    }

    // ITC_50_4 FIX: _Exchange_Gift_Not_Enough_Points_BVA
    Long diem = (long) qua.getDiem();
    Integer soLuong = (dto.getSoLuong() != null && dto.getSoLuong() > 0) ? dto.getSoLuong() : 1;
    Long diemCanTieu = diem * soLuong;

    // 3. Check điểm (now validates against total points needed)
    if (user.getTotalPoints() < diemCanTieu) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không đủ điểm để đổi quà");
    }

    // 4. Trừ điểm (based on total quantity)
    user.setTotalPoints(user.getTotalPoints() - diemCanTieu.intValue());
    userRepository.save(user);

    // 5. GỘP QUÀ
    Optional<DoiQua> existing =
            doiQuaRepository.findByUserIdAndQuaId(uid, dto.getQuaId());

    if (existing.isPresent()) {
        DoiQua item = existing.get();
        item.setSoLuong(item.getSoLuong() + soLuong);
        doiQuaRepository.save(item);

    } else {
        DoiQua newItem = new DoiQua();
        newItem.setUserId(uid);
        newItem.setQuaId(dto.getQuaId());
        newItem.setSoLuong(soLuong);

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