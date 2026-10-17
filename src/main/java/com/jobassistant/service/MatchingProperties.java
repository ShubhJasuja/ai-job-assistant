package com.jobassistant.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the matching engine.
 * All weights are externalized so they can be tuned without changing code.
 */
@Component
@ConfigurationProperties(prefix = "matching")
public class MatchingProperties {

    private int weightSkills = 40;
    private int weightExperience = 20;
    private int weightLocation = 15;
    private int weightEducation = 10;
    private int weightRemote = 10;
    private int weightRole = 5;

    public int getWeightSkills() { return weightSkills; }
    public void setWeightSkills(int weightSkills) { this.weightSkills = weightSkills; }

    public int getWeightExperience() { return weightExperience; }
    public void setWeightExperience(int weightExperience) { this.weightExperience = weightExperience; }

    public int getWeightLocation() { return weightLocation; }
    public void setWeightLocation(int weightLocation) { this.weightLocation = weightLocation; }

    public int getWeightEducation() { return weightEducation; }
    public void setWeightEducation(int weightEducation) { this.weightEducation = weightEducation; }

    public int getWeightRemote() { return weightRemote; }
    public void setWeightRemote(int weightRemote) { this.weightRemote = weightRemote; }

    public int getWeightRole() { return weightRole; }
    public void setWeightRole(int weightRole) { this.weightRole = weightRole; }
}