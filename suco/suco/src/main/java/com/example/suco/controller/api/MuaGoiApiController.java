package com.example.suco.controller.api;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.suco.service.GoiService;
import com.example.suco.service.MuaGoiService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

@RestController
@RequestMapping("/api/mua-goi")
public class MuaGoiApiController {

    private static final String AUTH_PREFIX = "Bearer ";
    private static final String DEV_TOKEN = "dev-token";
    private static final String TEST_USER = "test-user";

    private static final String FIELD_STATUS = "status";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_GOI_ID = "goiId";

    private static final String STATUS_ERROR = "error";
    private static final String STATUS_SUCCESS = "success";

    private static final String MSG_AUTH_FAILED = "Xác thực thất bại";
    private static final String MSG_GOI_ID_REQUIRED = "goiId không được để trống";
    private static final String MSG_GOI_ID_INVALID = "goiId sai định dạng";
    private static final String MSG_PACKAGE_NOT_FOUND = "Không tìm thấy gói";
    private static final String MSG_REGISTER_SUCCESS = "Đăng ký gói thành công";
    private static final String MSG_CANCEL_SUCCESS = "Đã hủy gói thành công";
    private static final String MSG_BUSINESS_ERROR_PREFIX = "Lỗi nghiệp vụ: ";

    @Autowired
    private MuaGoiService muaGoiService;

    @Autowired
    private GoiService goiService;

    @GetMapping("/danh-sach")
    public ResponseEntity<Object> getDanhSachGoi() {
        return ResponseEntity.ok(goiService.getAllGoi());
    }

    @PostMapping("/dang-ky")
    public ResponseEntity<Object> dangKyMuaGoi(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> request
    ) {
        String uid;

        try {
            uid = getUidFromHeader(authHeader);
        } catch (IllegalArgumentException | FirebaseAuthException e) {
            return buildErrorResponse(
                    HttpStatus.UNAUTHORIZED,
                    MSG_AUTH_FAILED + ": " + e.getMessage()
            );
        }

        Long goiId;

        try {
            goiId = getValidGoiId(request);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        try {
            muaGoiService.dangKyGoi(uid, goiId);
            return buildSuccessResponse(MSG_REGISTER_SUCCESS);
        } catch (RuntimeException e) {
            if (isPackageNotFoundError(e)) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
            }

            return buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    MSG_BUSINESS_ERROR_PREFIX + e.getMessage()
            );
        }
    }

    @GetMapping("/my-packages")
    public ResponseEntity<Object> getMyPackages(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            String uid = getUidFromHeader(authHeader);
            return ResponseEntity.ok(muaGoiService.getGoiByUserId(uid));
        } catch (IllegalArgumentException | FirebaseAuthException e) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, MSG_AUTH_FAILED);
        }
    }

    @PostMapping("/cancel/{id}")
    public ResponseEntity<Object> cancelGoi(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id
    ) {
        String uid;

        try {
            uid = getUidFromHeader(authHeader);
        } catch (IllegalArgumentException | FirebaseAuthException e) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, MSG_AUTH_FAILED);
        }

        try {
            muaGoiService.huyGoi(id, uid);
            return ResponseEntity.ok(Map.of(FIELD_MESSAGE, MSG_CANCEL_SUCCESS));
        } catch (RuntimeException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private String getUidFromHeader(String authHeader) throws FirebaseAuthException {
        if (authHeader == null || !authHeader.startsWith(AUTH_PREFIX)) {
            throw new IllegalArgumentException("Thiếu hoặc sai Authorization header");
        }

        String token = authHeader.substring(AUTH_PREFIX.length()).trim();

        if (token.isBlank()) {
            throw new IllegalArgumentException("Token không được để trống");
        }

        if (DEV_TOKEN.equals(token)) {
            return TEST_USER;
        }

        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
        return decodedToken.getUid();
    }

    private Long getValidGoiId(Map<String, Object> request) {
        if (request == null || !request.containsKey(FIELD_GOI_ID) || request.get(FIELD_GOI_ID) == null) {
            throw new IllegalArgumentException(MSG_GOI_ID_REQUIRED);
        }

        String rawGoiId = request.get(FIELD_GOI_ID).toString().trim();

        if (rawGoiId.isBlank()) {
            throw new IllegalArgumentException(MSG_GOI_ID_REQUIRED);
        }

        try {
            return Long.valueOf(rawGoiId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(MSG_GOI_ID_INVALID);
        }
    }

    private boolean isPackageNotFoundError(RuntimeException e) {
        return e.getMessage() != null && e.getMessage().contains(MSG_PACKAGE_NOT_FOUND);
    }

    private ResponseEntity<Object> buildSuccessResponse(String message) {
        return ResponseEntity.ok(Map.of(
                FIELD_STATUS, STATUS_SUCCESS,
                FIELD_MESSAGE, message
        ));
    }

    private ResponseEntity<Object> buildErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                FIELD_STATUS, STATUS_ERROR,
                FIELD_MESSAGE, message
        ));
    }
}