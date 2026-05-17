
package com.example.suco.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Service
public class GeminiAiService {

    @Value("${gemini.api.keys}")
    private String keys;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private List<String> apiKeys;
    private int currentIndex = 0;

    @PostConstruct
    public void init() {
        apiKeys = List.of(keys.split(","));
    }

    public AiVerifyResult verifyImage(String base64Image, String loaiSuCo) {

        // 0. Nếu không có ảnh
        if (base64Image == null || base64Image.isBlank()) {
            return new AiVerifyResult(true, 50, "Không có ảnh, bỏ qua kiểm tra AI");
        }

        // 1. Xử lý Base64
        if (base64Image.contains(",")) {
            base64Image = base64Image.split(",")[1];
        }

        String modelName = model.startsWith("models/") ? model.substring(7) : model;

        // 2. Prompt
        String prompt = """
        Bạn là AI duyệt ảnh cho HỆ THỐNG DEMO.
        Mục tiêu:
        - Chỉ cần ảnh CÓ LIÊN QUAN CHUNG đến loại sự cố "%s".
        - Ảnh minh họa, ảnh demo, ảnh internet đều ĐƯỢC CHẤP NHẬN.
        - Trả lời DUY NHẤT bằng JSON format:
        {
          "isValid": true | false,
          "confidence": 0-100,
          "reason": "ngắn gọn"
        }
        """.formatted(loaiSuCo);

        Map<String, Object> body = Map.of(
            "contents", List.of(
                Map.of(
                    "parts", List.of(
                        Map.of("text", prompt),
                        Map.of(
                            "inline_data", Map.of(
                                "mime_type", "image/jpeg",
                                "data", base64Image
                            )
                        )
                    )
                )
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 3. TRY ALL KEYS
        for (int i = 0; i < apiKeys.size(); i++) {

            String key = apiKeys.get((currentIndex + i) % apiKeys.size());

            try {
                String url = baseUrl + "/models/" + modelName + ":generateContent?key=" + key;

                System.out.println("🔑 DÙNG KEY: " + key);
                System.out.println("🔗 URL: " + url);

                ResponseEntity<Map> response =
                        restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);

                // Parse response
                List<Map<String, Object>> candidates =
                        (List<Map<String, Object>>) response.getBody().get("candidates");

                Map<String, Object> content =
                        (Map<String, Object>) candidates.get(0).get("content");

                List<Map<String, String>> parts =
                        (List<Map<String, String>>) content.get("parts");

                String jsonText = parts.get(0).get("text");

                System.out.println("🤖 RAW: " + jsonText);

                String cleanedJson = jsonText.replaceAll("(?s)```json\\s*|\\s*```", "").trim();

                AiVerifyResult result = mapper.readValue(cleanedJson, AiVerifyResult.class);

                if (result.getConfidence() == null || result.getConfidence() == 0) {
                    if (result.isValid()) result.setConfidence(45);
                }

                // update key đang dùng
                currentIndex = (currentIndex + i + 1) % apiKeys.size();

                return result;

            } catch (Exception e) {

                System.err.println("❌ KEY LỖI: " + key + " | " + e.getMessage());

                // nếu key lỗi → thử key tiếp theo
            }
        }

        // 4. Nếu tất cả key đều lỗi
        return new AiVerifyResult(
                true,
                50,
                "Tất cả API key đều lỗi → auto duyệt"
        );
    }
}