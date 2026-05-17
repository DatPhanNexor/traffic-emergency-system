package com.example.suco.dto;



// File: CallSignalDto.java tại Backend (Java thuần)
public class CallSignalDto {
    private String type;
    private String from;
    private String to;
    private String sdp;
    private Object candidate; // Dùng Object để nhận mọi loại dữ liệu ICE Candidate

    // BẮT BUỘC: Phải có Constructor không tham số
    public CallSignalDto() {}

    // Getter và Setter thủ công (Vì không dùng Lombok)
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getSdp() { return sdp; }
    public void setSdp(String sdp) { this.sdp = sdp; }

    public Object getCandidate() { return candidate; }
    public void setCandidate(Object candidate) { this.candidate = candidate; }
}