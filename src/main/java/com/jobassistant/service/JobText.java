package com.jobassistant.service;

import com.jobassistant.enums.RemoteStatus;

import java.util.Locale;
import java.util.regex.Pattern;

final class JobText {
    private static final Pattern HTML_TAGS = Pattern.compile("<[^>]*>");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private JobText() {}

    static String cleanHtml(String text) {
        if (text == null || text.isBlank()) return "";
        return SPACES.matcher(HTML_TAGS.matcher(text).replaceAll(" "))
                .replaceAll(" ")
                .trim();
    }

    static RemoteStatus remoteStatus(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        if (normalized.contains("hybrid")) return RemoteStatus.HYBRID;
        if (normalized.contains("remote") || normalized.contains("work from home")) return RemoteStatus.REMOTE;
        if (normalized.contains("on-site") || normalized.contains("onsite")) return RemoteStatus.ON_SITE;
        return null;
    }

    static String employmentType(String title, String description) {
        String normalized = (title + " " + description).toLowerCase(Locale.ROOT);
        if (normalized.contains("intern")) return "INTERNSHIP";
        if (normalized.contains("contract")) return "CONTRACT";
        if (normalized.contains("part-time") || normalized.contains("part time")) return "PART_TIME";
        if (normalized.contains("full-time") || normalized.contains("full time")) return "FULL_TIME";
        return null;
    }
}
