package com.jobassistant.service;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.net.URI;
import java.time.Duration;

final class JobSourceHttpClient {
    private JobSourceHttpClient() {}

    static RestClient create() {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(client);
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    static String get(RestClient client, URI uri) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return client.get().uri(uri).retrieve().body(String.class);
            } catch (RestClientResponseException exception) {
                int status = exception.getStatusCode().value();
                if ((status != 429 && status < 500) || attempt == 2) throw exception;
                pause(retryDelay(exception.getResponseHeaders(), attempt));
            }
        }
        throw new IllegalStateException("Job source request retries exhausted");
    }

    private static Duration retryDelay(HttpHeaders headers, int attempt) {
        String retryAfter = headers == null ? null : headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (retryAfter != null) {
            try {
                return Duration.ofSeconds(Math.min(2, Math.max(0, Long.parseLong(retryAfter))));
            } catch (NumberFormatException ignored) {
                // Fall back to a short exponential delay when Retry-After is an HTTP date.
            }
        }
        return Duration.ofMillis(250L << attempt);
    }

    private static void pause(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying a job source request", exception);
        }
    }
}
