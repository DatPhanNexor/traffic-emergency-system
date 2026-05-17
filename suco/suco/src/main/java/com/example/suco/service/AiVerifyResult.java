package com.example.suco.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AiVerifyResult {

    @JsonProperty("isValid")
    private boolean valid;  
    private Integer confidence;
    private String reason;
    private Double distance;
    
    public AiVerifyResult() {}

    public AiVerifyResult(boolean valid, Integer confidence, String reason, Double distance) {
        this.valid = valid;
        this.confidence = confidence;
        this.reason = reason;
        this.distance = distance;
    }
    public AiVerifyResult(boolean valid, Integer confidence, String reason) {
    this(valid, confidence, reason, null);
}

    // Getter
    public boolean isValid() { return valid; }
    public Integer getConfidence() { return confidence; }
    public String getReason() { return reason; }
    public Double getDistance() { return distance; }
    // Setter
    public void setValid(boolean valid) { this.valid = valid; }
    public void setConfidence(Integer confidence) { this.confidence = confidence; }
    public void setReason(String reason) { this.reason = reason; }
    public void setDistance(Double distance) { this.distance = distance; }
}
