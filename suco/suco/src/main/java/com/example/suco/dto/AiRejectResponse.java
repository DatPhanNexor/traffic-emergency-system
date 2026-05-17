package com.example.suco.dto;

public class AiRejectResponse {
    private String code;
    private String message;
    private int confidence;
    private Double distance;

    public AiRejectResponse(String code, String message, int confidence, Double distance) {
        this.code = code;
        this.message = message;
        this.confidence = confidence;
        this.distance = distance;
    }

    public AiRejectResponse(String code, String message, int confidence) {
    this.code = code;
    this.message = message;
    this.confidence = confidence;
}


    public String getCode() { return code; }
    public String getMessage() { return message; }
    public int getConfidence() { return confidence; }
    public Double getDistance() { return distance; }
}
