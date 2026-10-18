package com.jobassistant.dto;

public class CoverLetterResponse {
    private Long id;
    private String draftText;
    private String keyPoints;
    private Boolean approved;

    public CoverLetterResponse(Long id, String draftText, String keyPoints, Boolean approved) {
        this.id = id; this.draftText = draftText; this.keyPoints = keyPoints; this.approved = approved;
    }
    public Long getId() { return id; }
    public String getDraftText() { return draftText; }
    public String getKeyPoints() { return keyPoints; }
    public Boolean getApproved() { return approved; }
}
