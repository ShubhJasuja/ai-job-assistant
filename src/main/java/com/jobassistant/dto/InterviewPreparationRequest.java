package com.jobassistant.dto;

public class InterviewPreparationRequest {
    private Long resumeId;
    private Long jobId;
    private String suggestedQuestions;
    private String tips;

    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getSuggestedQuestions() { return suggestedQuestions; }
    public void setSuggestedQuestions(String suggestedQuestions) { this.suggestedQuestions = suggestedQuestions; }
    public String getTips() { return tips; }
    public void setTips(String tips) { this.tips = tips; }
}
