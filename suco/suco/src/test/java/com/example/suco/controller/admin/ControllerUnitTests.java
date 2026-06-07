package com.example.suco.controller.admin;

import com.example.suco.controller.admin.AdminUserController;
import com.example.suco.controller.admin.UserController;
import com.example.suco.model.User;
import com.example.suco.repository.UserRepository;
import com.example.suco.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class ControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void itc_3_1_getUserByUid_returns200_whenUserExists() throws Exception {
        User user = new User();
        user.setUid("test-user");
        user.setEmail("test@example.com");
        user.setName("Test User");

        when(userService.getUserInfo("test-user")).thenReturn(user);

        mockMvc.perform(get("/api/auth/test-user")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("test-user"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void itc_4_1_getAdminUsers_returns200_arrayOfUsers() throws Exception {
        User user = new User();
        user.setUid("admin-user");
        user.setEmail("admin@example.com");
        user.setName("Admin User");

        when(userService.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/admin/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uid").value("admin-user"))
                .andExpect(jsonPath("$[0].email").value("admin@example.com"));
    }

    @Test
    void itc_5_2_deleteUserEmptyUid_returns400_invalidUid() throws Exception {
        mockMvc.perform(delete("/admin/quan-ly-user/delete/")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_UID"));
    }
}
