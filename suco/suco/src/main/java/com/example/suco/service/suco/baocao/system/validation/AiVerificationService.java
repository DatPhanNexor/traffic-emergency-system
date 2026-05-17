package com.example.suco.service.suco.baocao.system.validation;

import com.example.suco.service.AiVerifyResult;
import com.example.suco.service.GeminiAiService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiVerificationService {

    @Autowired
    private GeminiAiService geminiAiService;

    public AiVerifyResult verifyReportImage(
            String base64FullData,
            String loaiSuCo
    ) {

        AiVerifyResult ai;

        if (base64FullData != null && !base64FullData.isBlank()) {

            ai = geminiAiService.verifyImage(
                    base64FullData,
                    loaiSuCo
            );

            System.out.println("=== AI RESULT ===");
            System.out.println("Valid: " + ai.isValid());
            System.out.println("Confidence: " + ai.getConfidence());
            System.out.println("Distance (before): " + ai.getDistance());

        } else {

            ai = new AiVerifyResult(
                    true,
                    50,
                    "Không có ảnh",
                    null
            );
        }

        return ai;
    }
}