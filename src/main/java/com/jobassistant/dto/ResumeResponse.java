package com.jobassistant.dto;

public class ResumeResponse {
    private Long id;
    private String title;
    private String content;
    private Boolean isBase;

    public ResumeResponse(Long id, String title, String content, Boolean isBase) {
        this.id = id; this.title = title; this.content = content; this.isBase = isBase;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Boolean getIsBase() { return isBase; }
    public void setIsBase(Boolean isBase) { this.isBase = isBase; }
}