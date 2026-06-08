package com.example.suco.service;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.example.suco.dto.DoiQuaDto;
import com.example.suco.model.DoiQua;
import com.example.suco.model.Qua;
import com.example.suco.model.User;
import com.example.suco.repository.DoiQuaRepository;
import com.example.suco.repository.QuaRepository;
import com.example.suco.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DoiQuaServiceTest {

    @Mock
    private DoiQuaRepository doiQuaRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuaRepository quaRepository;

    @InjectMocks
    private DoiQuaService doiQuaService;

    private User user;
    private Qua qua;
    private DoiQuaDto dto;

    private static final String USER_UID = "user-123";
    private static final Long QUA_ID = 1L;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUid(USER_UID);
        user.setTotalPoints(500);

        qua = new Qua();
        qua.setId(QUA_ID);
        qua.setTen("Quà tặng A");
        qua.setDiem(100);
        qua.setTrangThai(Qua.TrangThai.HOAT_DONG);
        qua.setNgayKetThuc(LocalDateTime.now().plusDays(30));

        dto = new DoiQuaDto();
        dto.setQuaId(QUA_ID);
        dto.setSoLuong(1);
    }

    // =========================================================================
    // TEST thucHienDoiQua - THÀNH CÔNG
    // =========================================================================

    @Test
    void testDoiQua_ThanhCong_SoLuong1_TaoMoiDoiQua() {
        // Đổi 1 quà, chưa có bản ghi DoiQua trước đó → tạo mới
        when(userRepository.findById(USER_UID)).thenReturn(Optional.of(user));
        when(quaRepository.findById(QUA_ID)).thenReturn(Optional.of(qua));
        when(doiQuaRepository.findByUserIdAndQuaId(USER_UID, QUA_ID)).thenReturn(Optional.empty());

        boolean result = doiQuaService.thucHienDoiQua(USER_UID, dto);

        assertTrue(result);
        assertEquals(400, user.getTotalPoints()); // 500 - 100*1 = 400
        verify(userRepository, times(1)).save(user);
        verify(doiQuaRepository, times(1)).save(any(DoiQua.class));
    }

    @Test
    void testDoiQua_ThanhCong_SoLuongNhieu_TaoMoiDoiQua() {
        // Đổi 3 quà cùng lúc, chưa có bản ghi → tạo mới
        dto.setSoLuong(3);
        when(userRepository.findById(USER_UID)).thenReturn(Optional.of(user));
        when(quaRepository.findById(QUA_ID)).thenReturn(Optional.of(qua));
        when(doiQuaRepository.findByUserIdAndQuaId(USER_UID, QUA_ID)).thenReturn(Optional.empty());

        boolean result = doiQuaService.thucHienDoiQua(USER_UID, dto);

        assertTrue(result);
        assertEquals(200, user.getTotalPoints()); // 500 - 100*3 = 200
        verify(userRepository, times(1)).save(user);
        verify(doiQuaRepository, times(1)).save(any(DoiQua.class));
    }

    @Test
    void testDoiQua_ThanhCong_GopQua_DaTonTaiDoiQua() {
        // Đổi 2 quà, đã có bản ghi DoiQua với soLuong = 1 → gộp thành 3
        dto.setSoLuong(2);
        DoiQua existingDoiQua = new DoiQua();
        existingDoiQua.setId(10L);
        existingDoiQua.setUserId(USER_UID);
        existingDoiQua.setQuaId(QUA_ID);
        existingDoiQua.setSoLuong(1);

        when(userRepository.findById(USER_UID)).thenReturn(Optional.of(user));
        when(quaRepository.findById(QUA_ID)).thenReturn(Optional.of(qua));
        when(doiQuaRepository.findByUserIdAndQuaId(USER_UID, QUA_ID)).thenReturn(Optional.of(existingDoiQua));

        boolean result = doiQuaService.thucHienDoiQua(USER_UID, dto);

        assertTrue(result);
        assertEquals(300, user.getTotalPoints()); // 500 - 100*2 = 300
        assertEquals(3, existingDoiQua.getSoLuong()); // 1 + 2 = 3
        verify(doiQuaRepository, times(1)).save(existingDoiQua);
    }

    @Test
    void testDoiQua_ThanhCong_SoLuongNull_MacDinh1() {
        // soLuong = null → mặc định là 1
        dto.setSoLuong(null);
        when(userRepository.findById(USER_UID)).thenReturn(Optional.of(user));
        when(quaRepository.findById(QUA_ID)).thenReturn(Optional.of(qua));
        when(doiQuaRepository.findByUserIdAndQuaId(USER_UID, QUA_ID)).thenReturn(Optional.empty());

        boolean result = doiQuaService.thucHienDoiQua(USER_UID, dto);

        assertTrue(result);
        assertEquals(400, user.getTotalPoints()); // 500 - 100*1 = 400
    }

    @Test
    void testDoiQua_ThanhCong_SoLuong0_MacDinh1() {
        // soLuong = 0 → mặc định là 1
        dto.setSoLuong(0);
        when(userRepository.findById(USER_UID)).thenReturn(Optional.of(user));
        when(quaRepository.findById(QUA_ID)).thenReturn(Optional.of(qua));
        when(doiQuaRepository.findByUserIdAndQuaId(USER_UID, QUA_ID)).thenReturn(Optional.empty());

        boolean result = doiQuaService.thucHienDoiQua(USER_UID, dto);

        assertTrue(result);
        assertEquals(400, user.getTotalPoints()); // 500 - 100*1 = 400
    }

    @Test
    void testDoiQua_ThanhCong_SoLuongAm_MacDinh1() {
        // soLuong = -1 → mặc định là 1 (vì <= 0)
        dto.setSoLuong(-1);
        when(userRepository.findById(USER_UID)).thenReturn(Optional.of(user));
        when(quaRepository.findById(QUA_ID)).thenReturn(Optional.of(qua));
        when(doiQuaRepository.findByUserIdAndQuaId(USER_UID, QUA_ID)).thenReturn(Optional.empty());

        boolean result = doiQuaService.thucHienDoiQua(USER_UID, dto);

        assertTrue(result);
        assertEquals(400, user.getTotalPoints()); // 500 - 100*1 = 400
    }

    @Test
    void testDoiQua_ThanhCong_DiemVuaDu() {
        // User có đúng 300 điểm, đổi 3 quà x 100 điểm = 300 → vừa đủ
        user.setTotalPoints(300);
        dto.setSoLuong(3);
        when(userRepository.findById(USER_UID)).thenReturn(Optional.of(user));
        when(quaRepository.findById(QUA_ID)).thenReturn(Optional.of(qua));
        when(doiQuaRepository.findByUserIdAndQuaId(USER_UID, QUA_ID)).thenReturn(Optional.empty());

        boolean result = doiQuaService.thucHienDoiQua(USER_UID, dto);

        assertTrue(result);
        assertEquals(0, user.getTotalPoints()); // 300 - 300 = 0
    }

    @Test
    void testDoiQua_ThanhCong_QuaKhongCoNgayKetThuc() {
        // Quà không có ngày kết thúc → không bị hết hạn
        qua.setNgayKetThuc(null);
        when(userRepository.findById(USER_UID)).thenReturn(Optional.of(user));
        when(quaRepository.findById(QUA_ID)).thenReturn(Optional.of(qua));
        when(doiQuaRepository.findByUserIdAndQuaId(USER_UID, QUA_ID)).thenReturn(Optional.empty());

        boolean result = doiQuaService.thucHienDoiQua(USER_UID, dto);

        assertTrue(result);
    }

    // =========================================================================
    // TEST thucHienDoiQua - THẤT BẠI: User không tồn tại
    // =========================================================================

    @Test
    void testDoiQua_ThatBai_UserKhongTonTai() {
        when(userRepository.findById(USER_UID)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> doiQuaService.thucHienDoiQua(USER_UID, dto));
        assertEquals("User không tồn tại", ex.getMessage());
        verify(doiQuaRepository, never()).save(any());
    }

    // =========================================================================
    // TEST thucHienDoiQua - THẤT BẠI: Quà không tồn tại
    // =========================================================================

    @Test
    void testDoiQua_ThatBai_QuaKhongTonTai() {
        when(userRepository.findById(USER_UID)).thenReturn(Optional.of(user));
        when(quaRepository.findById(QUA_ID)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> doiQuaService.thucHienDoiQua(USER_UID, dto));
        assertEquals("Quà không tồn tại", ex.getMessage());
        verify(doiQuaRepository, never()).save(any());
    }

    // =========================================================================
    // TEST thucHienDoiQua - THẤT BẠI: Quà không còn hoạt động (trạng thái NGUNG)
    // =========================================================================

    @Test
    void testDoiQua_ThatBai_QuaDaNgung() {
        qua.setTrangThai(Qua.TrangThai.NGUNG);
        when(userRepository.findById(USER_UID)).thenReturn(Optional.of(user));
        when(quaRepository.findById(QUA_ID)).thenReturn(Optional.of(qua));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> doiQuaService.thucHienDoiQua(USER_UID, dto));
        assertTrue(ex.getReason().contains("Quà không còn khả dụng"));
        verify(doiQuaRepository, never()).save(any());
    }

    // =========================================================================
    // TEST thucHienDoiQua - THẤT BẠI: Quà đã hết hạn
    // =========================================================================

    @Test
    void testDoiQua_ThatBai_QuaDaHetHan() {
        qua.setNgayKetThuc(LocalDateTime.now().minusDays(1));
        when(userRepository.findById(USER_UID)).thenReturn(Optional.of(user));
        when(quaRepository.findById(QUA_ID)).thenReturn(Optional.of(qua));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> doiQuaService.thucHienDoiQua(USER_UID, dto));
        assertEquals("Quà đã hết thời gian", ex.getMessage());
        verify(doiQuaRepository, never()).save(any());
    }

    // =========================================================================
    // TEST thucHienDoiQua - THẤT BẠI: Không đủ điểm (ITC_50_4)
    // =========================================================================

    @Test
    void testDoiQua_ThatBai_KhongDuDiem_SoLuong1() {
        // User có 50 điểm, quà cần 100 → thiếu
        user.setTotalPoints(50);
        when(userRepository.findById(USER_UID)).thenReturn(Optional.of(user));
        when(quaRepository.findById(QUA_ID)).thenReturn(Optional.of(qua));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> doiQuaService.thucHienDoiQua(USER_UID, dto));
        assertTrue(ex.getReason().contains("Không đủ điểm để đổi quà"));
        verify(doiQuaRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testDoiQua_ThatBai_KhongDuDiem_SoLuongNhieu() {
        // User có 250 điểm, đổi 3 quà x 100 = 300 → thiếu 50
        user.setTotalPoints(250);
        dto.setSoLuong(3);
        when(userRepository.findById(USER_UID)).thenReturn(Optional.of(user));
        when(quaRepository.findById(QUA_ID)).thenReturn(Optional.of(qua));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> doiQuaService.thucHienDoiQua(USER_UID, dto));
        assertTrue(ex.getReason().contains("Không đủ điểm để đổi quà"));
        verify(doiQuaRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testDoiQua_ThatBai_KhongDuDiem_BVA_ThieuMotDiem() {
        // BVA: User có 299 điểm, đổi 3 quà x 100 = 300 → thiếu đúng 1 điểm
        user.setTotalPoints(299);
        dto.setSoLuong(3);
        when(userRepository.findById(USER_UID)).thenReturn(Optional.of(user));
        when(quaRepository.findById(QUA_ID)).thenReturn(Optional.of(qua));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> doiQuaService.thucHienDoiQua(USER_UID, dto));
        assertTrue(ex.getReason().contains("Không đủ điểm để đổi quà"));
    }

    @Test
    void testDoiQua_ThatBai_KhongDuDiem_UserCo0Diem() {
        // User có 0 điểm
        user.setTotalPoints(0);
        when(userRepository.findById(USER_UID)).thenReturn(Optional.of(user));
        when(quaRepository.findById(QUA_ID)).thenReturn(Optional.of(qua));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> doiQuaService.thucHienDoiQua(USER_UID, dto));
        assertTrue(ex.getReason().contains("Không đủ điểm để đổi quà"));
    }

    // =========================================================================
    // TEST thucHienDoiQua - Kiểm tra tính toán diemCanTieu chính xác
    // =========================================================================

    @Test
    void testDoiQua_TinhToanDiemCanTieu_ChinhXac() {
        // Quà 150 điểm, đổi 2 → diemCanTieu = 300, user có 500 → còn 200
        qua.setDiem(150);
        dto.setSoLuong(2);
        when(userRepository.findById(USER_UID)).thenReturn(Optional.of(user));
        when(quaRepository.findById(QUA_ID)).thenReturn(Optional.of(qua));
        when(doiQuaRepository.findByUserIdAndQuaId(USER_UID, QUA_ID)).thenReturn(Optional.empty());

        doiQuaService.thucHienDoiQua(USER_UID, dto);

        assertEquals(200, user.getTotalPoints()); // 500 - 150*2 = 200
    }
}
