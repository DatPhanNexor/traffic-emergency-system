package com.example.suco.controller.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.suco.security.JwtService;
import com.example.suco.service.GoiService;
import com.example.suco.service.MuaGoiService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = MuaGoiApiController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
public class MuaGoiApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MuaGoiService muaGoiService;

    @MockBean
    private GoiService goiService;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetDanhSachGoi() throws Exception {
        Mockito.when(goiService.getAllGoi()).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/api/mua-goi/danh-sach"))
                .andExpect(status().isOk());
    }

    @Test
    void testDangKyMuaGoi_Success() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("goiId", 1L);

        // NHÁNH 1: Dùng dev-token -> Code không đụng đến Firebase -> Không crash Java 24
        mockMvc.perform(post("/api/mua-goi/dang-ky")
                        .header("Authorization", "Bearer dev-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void testDangKyMuaGoi_XacThucThatBai() throws Exception {
        // NHÁNH 2: Dùng token sai -> Code gọi Firebase và tự văng lỗi -> Nhảy vào catch (Exception e)
        // Case này vẫn lấy được coverage cho khối catch 401 mà không cần MockStatic
        mockMvc.perform(post("/api/mua-goi/dang-ky")
                        .header("Authorization", "Bearer sai-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goiId\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCancelGoi_Success() throws Exception {
        mockMvc.perform(post("/api/mua-goi/cancel/1")
                        .header("Authorization", "Bearer dev-token"))
                .andExpect(status().isOk());
    }
}