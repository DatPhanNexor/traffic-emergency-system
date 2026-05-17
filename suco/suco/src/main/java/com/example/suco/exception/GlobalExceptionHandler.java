package com.example.suco.exception;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import com.example.suco.dto.AiRejectResponse;

import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        // NOTE: Removed MaxUploadSizeExceededException handler to avoid imposing file-size limits here.
        // Server-level limits (if any) are controlled by application properties / container settings.

    @ExceptionHandler(AiRejectException.class)
    public ResponseEntity<Map<String, String>> handleAiReject(AiRejectException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "code", "AI_REJECTED",
                        "message", ex.getMessage()
                ));
    }

    // ID không tồn tại
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(Map.of(
                        "message", ex.getReason()
                ));
    }

    // ID sai định dạng (chuỗi thay vì số)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                    "message", "ID không hợp lệ"
            ));
}
// Tên bị trùng
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<Map<String, String>> handleDuplicate(DataIntegrityViolationException ex) {
    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                    "message", "Tên loại sự cố đã tồn tại"
            ));
}
@ExceptionHandler(MissingRequestHeaderException.class)
public ResponseEntity<Map<String, Object>> handleMissingHeader(MissingRequestHeaderException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "code", "UNAUTHORIZED",
                        "message", "Thiếu Header Authorization (Token)",
                        "confidence", 0
                ));
    
}
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
    // Lấy tất cả các lỗi và nối thành một chuỗi message
    String errorMessage = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));

    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                    "code", "VALIDATION_ERROR",
                    "message", errorMessage,
                    "confidence", 0
            ));
}
@ExceptionHandler(RuntimeException.class)
public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                    "message", ex.getMessage()
            ));
}
}
