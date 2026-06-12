package com.example.suco.exception;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String FIELD_CODE = "code";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_CONFIDENCE = "confidence";

    private static final String CODE_AI_REJECTED = "AI_REJECTED";
    private static final String CODE_UNAUTHORIZED = "UNAUTHORIZED";
    private static final String CODE_VALIDATION_ERROR = "VALIDATION_ERROR";

    private static final String MSG_INVALID_ID = "ID không hợp lệ";
    private static final String MSG_DUPLICATE_INCIDENT_TYPE = "Tên loại sự cố đã tồn tại";
    private static final String MSG_MISSING_AUTH_HEADER = "Thiếu Header Authorization (Token)";
    private static final String MSG_INVALID_JSON_FORMAT = "Định dạng dữ liệu không hợp lệ";

    private static final int DEFAULT_CONFIDENCE = 0;

    @ExceptionHandler(AiRejectException.class)
    public ResponseEntity<Map<String, String>> handleAiReject(AiRejectException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        FIELD_CODE, CODE_AI_REJECTED,
                        FIELD_MESSAGE, ex.getMessage()
                ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(Map.of(
                        FIELD_MESSAGE, ex.getReason()
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        FIELD_MESSAGE, MSG_INVALID_ID
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(DataIntegrityViolationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        FIELD_MESSAGE, MSG_DUPLICATE_INCIDENT_TYPE
                ));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(MissingRequestHeaderException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        FIELD_CODE, CODE_UNAUTHORIZED,
                        FIELD_MESSAGE, MSG_MISSING_AUTH_HEADER,
                        FIELD_CONFIDENCE, DEFAULT_CONFIDENCE
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        FIELD_CODE, CODE_VALIDATION_ERROR,
                        FIELD_MESSAGE, errorMessage,
                        FIELD_CONFIDENCE, DEFAULT_CONFIDENCE
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleInvalidJson(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        FIELD_MESSAGE, MSG_INVALID_JSON_FORMAT
                ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        FIELD_MESSAGE, ex.getMessage()
                ));
    }
}