package com.example.suco.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.example.suco.model.TruSo;
import com.example.suco.repository.TruSoRepository;

@ExtendWith(MockitoExtension.class)
class TruSoServiceTest {

    @Mock
    private TruSoRepository truSoRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private TruSoService truSoService;

    private TruSo validTruSo;

    @BeforeEach
    void setUp() {
        validTruSo = new TruSo();
        validTruSo.setTenTruSo("Trụ sở Quận 1");
        validTruSo.setTenDangNhap("adminq1");
        validTruSo.setMatKhau("Admin@123");
        validTruSo.setKinhDo(106.6);
        validTruSo.setViDo(10.7);
    }

    @Test
    void testSaveTruSo_Success() {
        when(truSoRepository.existsByTenDangNhap(anyString())).thenReturn(false);
        when(truSoRepository.save(any(TruSo.class))).thenReturn(validTruSo);

        TruSo saved = truSoService.saveTruSo(validTruSo);
        assertNotNull(saved);
        verify(truSoRepository, times(1)).save(any(TruSo.class));
    }

    @Test
    void testSaveTruSo_MissingCoordinates() {
        validTruSo.setKinhDo(null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> truSoService.saveTruSo(validTruSo));
        assertEquals("Kinh độ và vĩ độ không được để trống", ex.getMessage());
    }

    @Test
    void testSaveTruSo_NameTooLong() {
        validTruSo.setTenTruSo("A".repeat(256));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> truSoService.saveTruSo(validTruSo));
        assertTrue(ex.getMessage().contains("vượt quá 255 ký tự"));
    }

    @Test
    void testSaveTruSo_WeakPassword() {
        validTruSo.setMatKhau("123");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> truSoService.saveTruSo(validTruSo));
        assertTrue(ex.getMessage().contains("ít nhất 8 ký tự"));
    }

    @Test
    void testSaveTruSo_DuplicateUsername() {
        when(truSoRepository.existsByTenDangNhap("adminq1")).thenReturn(true);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> truSoService.saveTruSo(validTruSo));
        assertEquals("Tên đăng nhập trụ sở đã tồn tại", ex.getMessage());
    }
}