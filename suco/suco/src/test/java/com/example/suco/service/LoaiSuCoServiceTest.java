package com.example.suco.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.example.suco.model.LoaiSuCo;
import com.example.suco.repository.LoaiSuCoRepository;

@ExtendWith(MockitoExtension.class)
class LoaiSuCoServiceTest {

    @Mock
    private LoaiSuCoRepository repository;

    @InjectMocks
    private LoaiSuCoService service;

    private LoaiSuCo loaiSuCo;

    @BeforeEach
    void setUp() {
        loaiSuCo = new LoaiSuCo();
        loaiSuCo.setId(1L);
        loaiSuCo.setTen("Cháy nổ");
    }

    @Test
    void testCreateLoaiSuCo_Success() throws IOException {
        // Giả lập: Tên chưa tồn tại
        when(repository.findAllByTen("Sự cố mới")).thenReturn(new ArrayList<>());
        when(repository.save(any(LoaiSuCo.class))).thenReturn(loaiSuCo);

        LoaiSuCo result = service.createLoaiSuCo("Sự cố mới", null);

        assertNotNull(result);
        verify(repository, times(1)).save(any(LoaiSuCo.class));
    }

    @Test
    void testCreateLoaiSuCo_EmptyName() {
        // Kiểm tra trường hợp tên trống
        assertThrows(ResponseStatusException.class, () -> {
            service.createLoaiSuCo("", null);
        });
    }

    @Test
    void testCreateLoaiSuCo_DuplicateName() {
        // Giả lập: Tên đã tồn tại trong danh sách
        List<LoaiSuCo> list = new ArrayList<>();
        list.add(loaiSuCo);
        when(repository.findAllByTen("Cháy nổ")).thenReturn(list);

        assertThrows(ResponseStatusException.class, () -> {
            service.createLoaiSuCo("Cháy nổ", null);
        });
    }

    @Test
    void testUpdateLoaiSuCo_Success() throws IOException {
        when(repository.findById(1L)).thenReturn(Optional.of(loaiSuCo));
        when(repository.findAllByTen("Tên mới")).thenReturn(new ArrayList<>());
        when(repository.save(any(LoaiSuCo.class))).thenReturn(loaiSuCo);

        LoaiSuCo result = service.updateLoaiSuCo(1L, "Tên mới", null);

        assertNotNull(result);
        assertEquals("Tên mới", loaiSuCo.getTen());
    }

    @Test
    void testDeleteLoaiSuCo() {
        when(repository.findById(1L)).thenReturn(Optional.of(loaiSuCo));
        doNothing().when(repository).delete(loaiSuCo);

        assertDoesNotThrow(() -> service.deleteLoaiSuCo(1L));
        verify(repository, times(1)).delete(loaiSuCo);
    }
}