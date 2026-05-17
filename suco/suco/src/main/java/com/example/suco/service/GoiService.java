package com.example.suco.service;

import com.example.suco.dto.GoiDto;
import com.example.suco.model.Goi;
import com.example.suco.repository.GoiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GoiService {

    @Autowired
    private GoiRepository goiRepository;

    // Lấy danh sách gói và chuyển sang DTO
    public List<GoiDto> getAllGoi() {
        List<Goi> list = goiRepository.findAll();
        List<GoiDto> dtos = new ArrayList<>();
        for (Goi g : list) {
            dtos.add(convertToDto(g));
        }
        return dtos;
    }

    // Lưu hoặc cập nhật gói
    public void saveGoi(GoiDto dto) {
        Goi g = new Goi();
        if (dto.getId() != null) {
            g.setId(dto.getId());
        }
        g.setTen(dto.getTen());
        g.setGia(dto.getGia());
        g.setThoiHan(dto.getThoiHan());
        g.setKhoangCachMienPhi(dto.getKhoangCachMienPhi());
        g.setUuDai(dto.getUuDai());
        goiRepository.save(g);
    }

    // Xóa gói
    public void deleteGoi(Long id) {
    Goi goi = goiRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Gói không tồn tại"));

    // (OPTIONAL) Nếu có ràng buộc, check ở đây
    // ví dụ: không cho xóa nếu đang có người dùng
    // if (muaGoiRepository.existsByGoiId(id)) {
    //     throw new RuntimeException("Không thể xóa gói đang được sử dụng");
    // }

    goiRepository.delete(goi);
}

    // Hàm phụ chuyển đổi Entity -> DTO
    private GoiDto convertToDto(Goi g) {
        GoiDto dto = new GoiDto();
        dto.setId(g.getId());
        dto.setTen(g.getTen());
        dto.setGia(g.getGia());
        dto.setThoiHan(g.getThoiHan());
        dto.setKhoangCachMienPhi(g.getKhoangCachMienPhi());
        dto.setUuDai(g.getUuDai());
        return dto;
    }
//     public boolean deleteGoiWithCheck(Long id) {
//     if (!goiRepository.existsById(id)) {
//         return false;
//     }
//     goiRepository.deleteById(id);
//     return true;
// }
public Goi createGoi(GoiDto dto) {
    if (dto.getTen() == null || dto.getTen().isBlank()) {
        throw new RuntimeException("Tên gói không được để trống");
    }

    if (dto.getGia() == null) {
    throw new RuntimeException("Giá không được để trống");
}

if (dto.getGia().compareTo(BigDecimal.ZERO) <= 0) {
    throw new RuntimeException("Giá phải lớn hơn 0");
}

    if (dto.getThoiHan() == null) {
    throw new RuntimeException("Thời hạn không được để trống");
}

if (dto.getThoiHan() <= 0) {
    throw new RuntimeException("Thời hạn phải lớn hơn 0");
}

if (dto.getKhoangCachMienPhi() == null) {
    throw new RuntimeException("Khoảng cách miễn phí không được để trống");
}
if (dto.getKhoangCachMienPhi() < 0) {
    throw new RuntimeException("Khoảng cách miễn phí phải lớn hơn hoặc bằng 0");
}



    Goi g = new Goi();
    g.setTen(dto.getTen());
    g.setGia(dto.getGia());
    g.setThoiHan(dto.getThoiHan());
    g.setKhoangCachMienPhi(dto.getKhoangCachMienPhi());
    g.setUuDai(dto.getUuDai());

    return goiRepository.save(g);
}

public void updateGoi(Long id, GoiDto dto) {
    Goi goi = goiRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Không tìm thấy gói"));

    if (dto.getTen() != null) {
        goi.setTen(dto.getTen());
    }

    if (dto.getGia() != null) {
        if (dto.getGia().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá phải lớn hơn 0");
        }
        goi.setGia(dto.getGia());
    }

    if (dto.getThoiHan() != null) {
        if (dto.getThoiHan() <= 0) {
            throw new RuntimeException("Thời hạn phải lớn hơn 0");
        }
        goi.setThoiHan(dto.getThoiHan());
    }

    // if (dto.getKhoangCachMienPhi() != null) {
    //     if (dto.getKhoangCachMienPhi() < 0) {
    //         throw new RuntimeException("Khoảng cách miễn phí phải lớn hơn hoặc bằng 0");
    //     }
    //     goi.setKhoangCachMienPhi(dto.getKhoangCachMienPhi());
    // }

    if (dto.getUuDai() != null) {
        goi.setUuDai(dto.getUuDai());
    }

    goiRepository.save(goi);
}
}