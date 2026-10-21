package com.jobassistant.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "job-sources")
public class JobSourceProperties {
    private List<String> greenhouseBoards = new ArrayList<>();
    private List<String> leverSites = new ArrayList<>();
    private List<String> rssFeeds = new ArrayList<>();
    private long refreshIntervalMs = 21_600_000;

    public List<String> getGreenhouseBoards() { return greenhouseBoards; }
    public void setGreenhouseBoards(List<String> greenhouseBoards) { this.greenhouseBoards = greenhouseBoards; }
    public List<String> getLeverSites() { return leverSites; }
    public void setLeverSites(List<String> leverSites) { this.leverSites = leverSites; }
    public List<String> getRssFeeds() { return rssFeeds; }
    public void setRssFeeds(List<String> rssFeeds) { this.rssFeeds = rssFeeds; }
    public long getRefreshIntervalMs() { return refreshIntervalMs; }
    public void setRefreshIntervalMs(long refreshIntervalMs) { this.refreshIntervalMs = refreshIntervalMs; }
}
