package com.example.suco.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.suco.dto.QuaDto;
import com.example.suco.model.Qua;
import com.example.suco.repository.QuaRepository;

@ExtendWith(MockitoExtension.class)
class QuaServiceTest {

    @Mock
    private QuaRepository quaRepository;

    @InjectMocks
    private QuaService quaService;

    private QuaDto quaDto;

    @BeforeEach
    void setUp() {
        quaDto = new QuaDto();
        quaDto.setTen("Quà test mới");
        quaDto.setLoai(Qua.LoaiQua.SAN_PHAM);
        quaDto.setMoTa("Mô tả quà test");
        quaDto.setDiem(100);
        quaDto.setNgayKetThuc(LocalDateTime.now().plusDays(30));
    }

    // =========================================================================
    // TEST addQua - THÀNH CÔNG
    // =========================================================================

    @Test
    void testAddQua_ThanhCong_SanPham() throws IOException {
        // Thêm quà loại SAN_PHAM, không có ảnh
        when(quaRepository.existsByTenIgnoreCase("Quà test mới")).thenReturn(false);

        assertDoesNotThrow(() -> quaService.addQua(quaDto));

        verify(quaRepository, times(1)).save(any(Qua.class));
    }

    @Test
    void testAddQua_ThanhCong_Voucher_CoGiaTriGiam() throws IOException {
        // Thêm quà loại VOUCHER với giá trị giảm và giá trị tối đa
        quaDto.setLoai(Qua.LoaiQua.VOUCHER);
        quaDto.setGiaTriGiamPercent(20);
        quaDto.setGiaTriToiDa(new BigDecimal("50000"));
        when(quaRepository.existsByTenIgnoreCase("Quà test mới")).thenReturn(false);

        assertDoesNotThrow(() -> quaService.addQua(quaDto));

        verify(quaRepository, times(1)).save(any(Qua.class));
    }

    @Test
    void testAddQua_ThanhCong_DiemBang1() throws IOException {
        // BVA: Điểm = 1 (giá trị biên nhỏ nhất hợp lệ)
        quaDto.setDiem(1);
        when(quaRepository.existsByTenIgnoreCase("Quà test mới")).thenReturn(false);

        assertDoesNotThrow(() -> quaService.addQua(quaDto));

        verify(quaRepository, times(1)).save(any(Qua.class));
    }

    @Test
    void testAddQua_ThanhCong_KhongCoNgayKetThuc() throws IOException {
        // Quà không có ngày kết thúc
        quaDto.setNgayKetThuc(null);
        when(quaRepository.existsByTenIgnoreCase("Quà test mới")).thenReturn(false);

        assertDoesNotThrow(() -> quaService.addQua(quaDto));

        verify(quaRepository, times(1)).save(any(Qua.class));
    }

    // =========================================================================
    // TEST addQua - THẤT BẠI: Tên null (ITC_47.4)
    // =========================================================================

    @Test
    void testAddQua_ThatBai_TenNull() {
        quaDto.setTen(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> quaService.addQua(quaDto));
        assertEquals("Tên quà không được để trống", ex.getMessage());
        verify(quaRepository, never()).save(any());
    }

    // =========================================================================
    // TEST addQua - THẤT BẠI: Tên rỗng (ITC_47.4)
    // =========================================================================

    @Test
    void testAddQua_ThatBai_TenRong() {
        quaDto.setTen("");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> quaService.addQua(quaDto));
        assertEquals("Tên quà không được để trống", ex.getMessage());
        verify(quaRepository, never()).save(any());
    }

    @Test
    void testAddQua_ThatBai_TenChiCoKhoangTrang() {
        quaDto.setTen("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> quaService.addQua(quaDto));
        assertEquals("Tên quà không được để trống", ex.getMessage());
        verify(quaRepository, never()).save(any());
    }

    // =========================================================================
    // TEST addQua - THẤT BẠI: Tên trùng lặp (ITC_47.4)
    // =========================================================================

    @Test
    void testAddQua_ThatBai_TenTrungLap() {
        when(quaRepository.existsByTenIgnoreCase("Quà test mới")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> quaService.addQua(quaDto));
        assertEquals("Tên quà đã tồn tại", ex.getMessage());
        verify(quaRepository, never()).save(any());
    }

    @Test
    void testAddQua_ThatBai_TenTrungLap_IgnoreCase() {
        // Kiểm tra trùng lặp không phân biệt hoa thường
        quaDto.setTen("QUÀ TEST MỚI");
        when(quaRepository.existsByTenIgnoreCase("QUÀ TEST MỚI")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> quaService.addQua(quaDto));
        assertEquals("Tên quà đã tồn tại", ex.getMessage());
        verify(quaRepository, never()).save(any());
    }

    // =========================================================================
    // TEST addQua - THẤT BẠI: Điểm null
    // =========================================================================

    @Test
    void testAddQua_ThatBai_DiemNull() {
        quaDto.setDiem(null);
        when(quaRepository.existsByTenIgnoreCase("Quà test mới")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> quaService.addQua(quaDto));
        assertEquals("Điểm đổi quà không được để trống", ex.getMessage());
        verify(quaRepository, never()).save(any());
    }

    // =========================================================================
    // TEST addQua - THẤT BẠI: Điểm <= 0
    // =========================================================================

    @Test
    void testAddQua_ThatBai_DiemBang0() {
        quaDto.setDiem(0);
        when(quaRepository.existsByTenIgnoreCase("Quà test mới")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> quaService.addQua(quaDto));
        assertEquals("Điểm đổi quà phải lớn hơn 0", ex.getMessage());
        verify(quaRepository, never()).save(any());
    }

    @Test
    void testAddQua_ThatBai_DiemAm() {
        quaDto.setDiem(-10);
        when(quaRepository.existsByTenIgnoreCase("Quà test mới")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> quaService.addQua(quaDto));
        assertEquals("Điểm đổi quà phải lớn hơn 0", ex.getMessage());
        verify(quaRepository, never()).save(any());
    }

    @Test
    void testAddQua_ThatBai_DiemBangAm1_BVA() {
        // BVA: Điểm = -1 (giá trị biên ngay dưới 0)
        quaDto.setDiem(-1);
        when(quaRepository.existsByTenIgnoreCase("Quà test mới")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> quaService.addQua(quaDto));
        assertEquals("Điểm đổi quà phải lớn hơn 0", ex.getMessage());
        verify(quaRepository, never()).save(any());
    }

    // =========================================================================
    // TEST deleteQua
    // =========================================================================

    @Test
    void testDeleteQua_ThanhCong() {
        Qua qua = new Qua();
        qua.setId(1L);
        when(quaRepository.findById(1L)).thenReturn(Optional.of(qua));

        assertDoesNotThrow(() -> quaService.deleteQua(1L));
        verify(quaRepository, times(1)).delete(qua);
    }

    @Test
    void testDeleteQua_ThatBai_KhongTimThay() {
        when(quaRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> quaService.deleteQua(999L));
        assertEquals("Không tìm thấy quà với id = 999", ex.getMessage());
    }

    // =========================================================================
    // TEST updateQua
    // =========================================================================

    @Test
    void testUpdateQua_ThanhCong_CapNhatTen() {
        Qua existingQua = new Qua();
        existingQua.setId(1L);
        existingQua.setTen("Quà cũ");

        QuaDto updateDto = new QuaDto();
        updateDto.setTen("Quà mới cập nhật");

        when(quaRepository.findById(1L)).thenReturn(Optional.of(existingQua));

        assertDoesNotThrow(() -> quaService.updateQua(1L, updateDto));
        assertEquals("Quà mới cập nhật", existingQua.getTen());
        verify(quaRepository, times(1)).save(existingQua);
    }

    @Test
    void testUpdateQua_ThanhCong_CapNhatVoucher() {
        Qua existingQua = new Qua();
        existingQua.setId(1L);
        existingQua.setLoai(Qua.LoaiQua.SAN_PHAM);

        QuaDto updateDto = new QuaDto();
        updateDto.setLoai(Qua.LoaiQua.VOUCHER);
        updateDto.setGiaTriGiamPercent(15);
        updateDto.setGiaTriToiDa(new BigDecimal("100000"));

        when(quaRepository.findById(1L)).thenReturn(Optional.of(existingQua));

        assertDoesNotThrow(() -> quaService.updateQua(1L, updateDto));
        assertEquals(Qua.LoaiQua.VOUCHER, existingQua.getLoai());
        assertEquals(15, existingQua.getGiaTriGiamPercent());
        assertEquals(new BigDecimal("100000"), existingQua.getGiaTriToiDa());
        verify(quaRepository, times(1)).save(existingQua);
    }

    @Test
    void testUpdateQua_ThatBai_KhongTimThay() {
        QuaDto updateDto = new QuaDto();
        updateDto.setTen("Tên mới");
        when(quaRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> quaService.updateQua(999L, updateDto));
        assertEquals("Không tìm thấy quà", ex.getMessage());
    }

    // =========================================================================
    // TEST updateStatus
    // =========================================================================

    @Test
    void testUpdateStatus_ThanhCong_NgungHoatDong() {
        Qua qua = new Qua();
        qua.setId(1L);
        qua.setTrangThai(Qua.TrangThai.HOAT_DONG);

        when(quaRepository.findById(1L)).thenReturn(Optional.of(qua));

        assertDoesNotThrow(() -> quaService.updateStatus(1L, Qua.TrangThai.NGUNG));
        assertEquals(Qua.TrangThai.NGUNG, qua.getTrangThai());
        verify(quaRepository, times(1)).save(qua);
    }

    @Test
    void testUpdateStatus_ThanhCong_KichHoatLai() {
        Qua qua = new Qua();
        qua.setId(1L);
        qua.setTrangThai(Qua.TrangThai.NGUNG);

        when(quaRepository.findById(1L)).thenReturn(Optional.of(qua));

        assertDoesNotThrow(() -> quaService.updateStatus(1L, Qua.TrangThai.HOAT_DONG));
        assertEquals(Qua.TrangThai.HOAT_DONG, qua.getTrangThai());
        verify(quaRepository, times(1)).save(qua);
    }

    @Test
    void testUpdateStatus_ThatBai_KhongTimThay() {
        when(quaRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> quaService.updateStatus(999L, Qua.TrangThai.NGUNG));
        assertEquals("Không tìm thấy", ex.getMessage());
    }
}
