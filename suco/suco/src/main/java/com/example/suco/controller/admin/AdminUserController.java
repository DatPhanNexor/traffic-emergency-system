// package com.example.suco.controller.admin;

// import com.example.suco.model.User;
// import com.example.suco.service.UserService;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import java.util.List;

// @RestController
// @RequestMapping("/api/admin/users")
// public class AdminUserController {
    

// 	private final UserService userService;

// 	public AdminUserController(UserService userService) {
// 		this.userService = userService;
// 	}

// 	@GetMapping
// 	public ResponseEntity<List<User>> getAllUsers() {
// 		return ResponseEntity.ok(userService.getAllUsers());
// 	}
// }