package com.example.suco.controller.api;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.suco.model.TruSo;
import com.example.suco.repository.UserRepository;
import com.example.suco.security.JwtService;
import com.example.suco.service.TruSoService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(TruSoApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class TruSoApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TruSoService truSoService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void testCreateTruSo_JsonSuccess() throws Exception {
        // 1. Chuẩn bị dữ liệu giả lập cho DTO (Khớp với TruSoRequest trong Controller)
        TruSoApiController.TruSoRequest request = new TruSoApiController.TruSoRequest();
        request.setTenTruSo("Trụ sở Test JSON");
        request.setTenDangNhap("adminjson");
        request.setMatKhau("Admin@123");
        request.setKinhDo(106.0);
        request.setViDo(10.0);

        // 2. Chuẩn bị dữ liệu trả về giả lập (Entity)
        TruSo savedTruSo = new TruSo();
        savedTruSo.setId(1L);
        savedTruSo.setTenTruSo("Trụ sở Test JSON");
        savedTruSo.setTenDangNhap("adminjson");

        // 3. Thiết lập Mockito: Khi gọi service.saveTruSo thì trả về savedTruSo
        when(truSoService.saveTruSo(any(TruSo.class))).thenReturn(savedTruSo);

        // 4. Thực hiện lệnh gọi API và kiểm tra kết quả
        mockMvc.perform(post("/api/tru-so")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request))) // Gửi DTO đi
                .andExpect(status().isCreated()) // Mong đợi mã 201
                .andExpect(jsonPath("$.message").value("Thêm trụ sở thành công!")) // Kiểm tra message trả về
                .andExpect(jsonPath("$.tenDangNhap").value("adminjson")); // Kiểm tra dữ liệu
    }
}
