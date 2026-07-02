package com.example.suco.service.xacthuc.user.token;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class FirebaseService {

    public String extractUid(String authHeader) throws FirebaseAuthException {

        if (authHeader == null || authHeader.isBlank()) {
            throw new RuntimeException("Authorization header không tồn tại");
        }

        String token = authHeader.replace("Bearer ", "");

        // Token giả để test Postman/dev
        if (token != null && token.startsWith("dev-token")) {
            if ("dev-token".equals(token)) {
                return "test-user";
            }
            return token.replace("dev-token", "test-user");
        }

        try {
            if (com.google.firebase.FirebaseApp.getApps().isEmpty()) {
                throw new IllegalStateException("FirebaseApp DEFAULT has not been initialized");
            }
            FirebaseToken decodedToken =
                    FirebaseAuth.getInstance().verifyIdToken(token);
            return decodedToken.getUid();
        } catch (Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Xác thực thất bại: " + e.getMessage()
            );
        }
    }
}