package com.jobassistant.dto;

public class InterviewPreparationResponse {
    private Long id;
    private Long resumeId;
    private Long jobId;
    private String suggestedQuestions;
    private String tips;

    public InterviewPreparationResponse(Long id, Long resumeId, Long jobId, String suggestedQuestions, String tips) {
        this.id = id; this.resumeId = resumeId; this.jobId = jobId;
        this.suggestedQuestions = suggestedQuestions; this.tips = tips;
    }
    public Long getId() { return id; }
    public Long getResumeId() { return resumeId; }
    public Long getJobId() { return jobId; }
    public String getSuggestedQuestions() { return suggestedQuestions; }
    public String getTips() { return tips; }
}
