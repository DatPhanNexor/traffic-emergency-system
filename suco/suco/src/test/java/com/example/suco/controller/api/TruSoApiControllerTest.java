package com.example.suco.controller.api;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.suco.dto.TruSoMapDto;
import com.example.suco.model.TruSo;
import com.example.suco.repository.UserRepository;
import com.example.suco.security.JwtService;
import com.example.suco.service.TruSoService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(TruSoApiController.class)
@AutoConfigureMockMvc(addFilters = false) // Tắt bảo mật để tập trung test logic API
class TruSoApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TruSoService truSoService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateTruSo_JsonSuccess() throws Exception {
        // 1. Chuẩn bị DTO đầu vào
        TruSoApiController.TruSoRequest request = new TruSoApiController.TruSoRequest();
        request.setTenTruSo("Trụ sở Quận 1");
        request.setTenDangNhap("adminq1");
        request.setMatKhau("Admin@123");
        request.setKinhDo(106.0);
        request.setViDo(10.0);

        // 2. Chuẩn bị Entity trả về từ Service
        TruSo savedTruSo = new TruSo();
        savedTruSo.setId(1L);
        savedTruSo.setTenTruSo("Trụ sở Quận 1");
        savedTruSo.setTenDangNhap("adminq1");

        when(truSoService.saveTruSo(any(TruSo.class))).thenReturn(savedTruSo);

        // 3. Thực thi request và Assert
        mockMvc.perform(post("/api/tru-so")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Thêm trụ sở thành công!"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testCreateTruSo_Failure() throws Exception {
        // Giả lập trường hợp Service ném ra lỗi (ví dụ trùng tên)
        when(truSoService.saveTruSo(any(TruSo.class))).thenThrow(new RuntimeException("Tên đăng nhập đã tồn tại"));

        TruSoApiController.TruSoRequest request = new TruSoApiController.TruSoRequest();
        request.setTenDangNhap("trungten");

        mockMvc.perform(post("/api/tru-so")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(pm -> {
                    String content = pm.getResponse().getContentAsString();
                    assert(content.contains("Tên đăng nhập đã tồn tại"));
                });
    }

    @Test
    void testGetAllTruSo_Success() throws Exception {
        // Giả lập danh sách trụ sở cho bản đồ
        TruSoMapDto dto = new TruSoMapDto(1L, "Trụ sở A", 106.0, 10.0);
        List<TruSoMapDto> list = Arrays.asList(dto);

        when(truSoService.getAllTruSoForMap()).thenReturn(list);

        mockMvc.perform(get("/api/tru-so/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tenTruSo").value("Trụ sở A"));
    }
}