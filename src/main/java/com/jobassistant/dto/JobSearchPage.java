package com.jobassistant.dto;

import java.util.List;

public record JobSearchPage<T>(List<T> items, long totalElements, boolean hasNext) {
}
