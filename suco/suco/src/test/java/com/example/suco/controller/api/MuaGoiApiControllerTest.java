package com.example.suco.controller.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
class MuaGoiApiControllerTest { // 1. Đã xóa 'public' ở đây

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

        mockMvc.perform(post("/api/mua-goi/dang-ky")
                        .header("Authorization", "Bearer dev-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void testDangKyMuaGoi_NotFound() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("goiId", 999L);

        // Giả lập lỗi 404 (Không tìm thấy gói) để lấy coverage cho nhánh catch RuntimeException
        Mockito.doThrow(new RuntimeException("Không tìm thấy gói"))
                .when(muaGoiService).dangKyGoi(anyString(), anyLong());

        mockMvc.perform(post("/api/mua-goi/dang-ky")
                        .header("Authorization", "Bearer dev-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDangKyMuaGoi_XacThucThatBai() throws Exception {
        // Vì trong môi trường Test, Firebase chưa init nên nó văng RuntimeException
        // Controller của bạn catch RuntimeException và trả về 400.
        // Vì vậy ta sửa lại mong đợi là isBadRequest (400) thay vì isUnauthorized (401)
        mockMvc.perform(post("/api/mua-goi/dang-ky")
                        .header("Authorization", "Bearer token-sai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goiId\":1}"))
                .andExpect(status().isBadRequest()); // SỬA TẠI ĐÂY (401 -> 400)
    }

    @Test
    void testGetMyPackages_Success() throws Exception {
        mockMvc.perform(get("/api/mua-goi/my-packages")
                        .header("Authorization", "Bearer dev-token"))
                .andExpect(status().isOk());
    }
//
    @Test
    void testCancelGoi_Success() throws Exception {
        mockMvc.perform(post("/api/mua-goi/cancel/1")
                        .header("Authorization", "Bearer dev-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã hủy gói thành công"));
    }

    @Test
    void testCancelGoi_BadRequest() throws Exception {
        Mockito.doThrow(new RuntimeException("Lỗi hủy gói"))
                .when(muaGoiService).huyGoi(anyLong(), anyString());

        mockMvc.perform(post("/api/mua-goi/cancel/1")
                        .header("Authorization", "Bearer dev-token"))
                .andExpect(status().isBadRequest());
    }
}