package com.jobassistant.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to generate or reuse a Correlation ID for tracing requests.
 * Uses SLF4J MDC (Mapped Diagnostic Context) so the ID is automatically
 * added to all log statements for the duration of the request.
 */
@Component
@Order(1)
public class CorrelationIdFilter implements Filter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_LOG_VAR = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String correlationId = req.getHeader(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Put into MDC for logging
        MDC.put(CORRELATION_ID_LOG_VAR, correlationId);

        try {
            // Also return the ID in the response for the client to trace
            if (response instanceof HttpServletResponse httpResponse) {
                httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
            }
            chain.doFilter(request, response);
        } finally {
            // Important: always clear MDC to prevent memory leaks in thread pools!
            MDC.remove(CORRELATION_ID_LOG_VAR);
        }
    }
}