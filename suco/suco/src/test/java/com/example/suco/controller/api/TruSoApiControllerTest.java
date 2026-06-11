package com.example.suco.controller.api;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when; // Kiểm tra lại đường dẫn này nếu bị đỏ
import org.springframework.beans.factory.annotation.Autowired; // Kiểm tra lại đường dẫn này nếu bị đỏ
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        TruSo truSo = new TruSo();
        truSo.setId(1L);
        truSo.setTenTruSo("Test JSON");
        truSo.setTenDangNhap("adminjson");
        truSo.setMatKhau("Admin@123");
        truSo.setKinhDo(106.0);
        truSo.setViDo(10.0);

        // Chỉnh lại cú pháp Mockito chuẩn
        when(truSoService.saveTruSo(any(TruSo.class))).thenReturn(truSo);

        mockMvc.perform(post("/api/tru-so")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(truSo)))
                .andExpect(status().isCreated());
    }
}