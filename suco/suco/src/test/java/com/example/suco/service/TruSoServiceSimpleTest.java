package com.example.suco.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.example.suco.model.TruSo;
import com.example.suco.repository.TruSoRepository;

@ExtendWith(MockitoExtension.class)
class TruSoServiceSimpleTest {

    @Mock
    private TruSoRepository truSoRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private TruSoService truSoService;

    @Test
    void testCoverageBypass() {
        // 1. Test trường hợp thiếu tọa độ (Pass ITC_6.3)
        TruSo tsNull = new TruSo();
        assertThrows(ResponseStatusException.class, () -> truSoService.saveTruSo(tsNull));

        // 2. Test trường hợp tên quá dài (Pass ITC_6.4)
        TruSo tsLongName = new TruSo();
        tsLongName.setKinhDo(106.0);
        tsLongName.setViDo(10.0);
        tsLongName.setTenTruSo("A".repeat(256));
        assertThrows(ResponseStatusException.class, () -> truSoService.saveTruSo(tsLongName));

        // 3. Test trường hợp mật khẩu yếu (Pass B04)
        TruSo tsWeakPass = new TruSo();
        tsWeakPass.setKinhDo(106.0);
        tsWeakPass.setViDo(10.0);
        tsWeakPass.setTenTruSo("Trụ sở Test");
        tsWeakPass.setTenDangNhap("admin123");
        tsWeakPass.setMatKhau("123");
        assertThrows(ResponseStatusException.class, () -> truSoService.saveTruSo(tsWeakPass));

        // 4. Test thành công để phủ nốt các dòng còn lại
        TruSo tsSuccess = new TruSo();
        tsSuccess.setKinhDo(106.0);
        tsSuccess.setViDo(10.0);
        tsSuccess.setTenTruSo("Trụ sở OK");
        tsSuccess.setTenDangNhap("adminok");
        tsSuccess.setMatKhau("Admin@123");

        when(truSoRepository.existsByTenDangNhap(any())).thenReturn(false);
        when(truSoRepository.save(any())).thenReturn(tsSuccess);

        assertDoesNotThrow(() -> truSoService.saveTruSo(tsSuccess));
    }
}