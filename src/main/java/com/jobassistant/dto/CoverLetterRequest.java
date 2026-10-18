package com.jobassistant.dto;

public class CoverLetterRequest {
    private Long resumeId;
    private Long jobId;
    // optional: user can specify focus

    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
}
