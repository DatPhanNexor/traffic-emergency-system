package com.example.suco.mapper;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.suco.dto.SuCoMapDto;
import com.example.suco.model.BaoCaoSuCo;
import com.example.suco.util.GeocodingUtil;

@Component
public class SuCoMapDtoMapper {

    @Autowired
    private GeocodingUtil geocodingUtil;

    /**
     * Thêm thông tin địa chỉ vào DTO từ kinh độ/vĩ độ (chỉ cho hiển thị)
     * @param dto DTO từ database
     * @return DTO với thông tin địa chỉ
     */
    public SuCoMapDto toDto(BaoCaoSuCo b) {
    if (b == null) return null;

    String tenLoai = (b.getLoaiSuCo() != null) ? b.getLoaiSuCo().getTen() : "Không xác định";
    String iconUrl = (b.getLoaiSuCo() != null) ? b.getLoaiSuCo().getIconUrl() : "";
    String tenNguoiBao = (b.getReporter() != null) ? b.getReporter().getName() : "Người dùng ẩn danh";

    // Truyền b.getDoTinCay() vào constructor
    return new SuCoMapDto(
        b.getId(), 
        b.getViDo(), 
        b.getKinhDo(), 
        b.getMoTa(), 
        tenLoai,
        b.getTrangThaiDuyet().toString(), // Đảm bảo là String
        b.getTrangThaiXuLy().toString(), 
        iconUrl,
        b.getMucDoNghiemTrong().toString(), 
        b.getHinhAnhUrl(),
        b.getDoTinCay(),        // <--- THÊM DÒNG NÀY (Độ tin cậy)
        null, null, null, null, // tenDuong, quan, huyen, thanhPho
        b.getDiaChi(),          // diaChi chuỗi
        tenNguoiBao             // tenNguoiBao
    );
}
    public SuCoMapDto enrichWithAddress(SuCoMapDto dto) {
        if (dto != null && dto.getViDo() != null && dto.getKinhDo() != null) {
            Map<String, String> addressMap = geocodingUtil.getAddressFromCoordinates(
                    dto.getViDo(), dto.getKinhDo());
            
            dto.setTenDuong(addressMap.get("tenDuong"));
            dto.setQuan(addressMap.get("quan"));
            dto.setHuyenHoac(addressMap.get("huyenHoac"));
            dto.setThanhPho(addressMap.get("thanhPho"));
            dto.setDiaChi(geocodingUtil.formatAddress(addressMap));
        }
        return dto;
    }
}
