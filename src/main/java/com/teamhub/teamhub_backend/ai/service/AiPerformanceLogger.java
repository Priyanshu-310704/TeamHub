package com.teamhub.teamhub_backend.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Structured performance logger for AI request lifecycle.
 * Thread-safe: each request creates its own context instance.
 */
@Slf4j
@Component
public class AiPerformanceLogger {

    public PerfContext start(String operation) {
        return new PerfContext(operation);
    }

    public static class PerfContext {
        private final String requestId;
        private final String operation;
        private final long startTime;
        private final Map<String, Long> timings = new LinkedHashMap<>();
        private long lastCheckpoint;

        PerfContext(String operation) {
            this.requestId = UUID.randomUUID().toString().substring(0, 8);
            this.operation = operation;
            this.startTime = System.nanoTime();
            this.lastCheckpoint = startTime;
        }

        public String getRequestId() {
            return requestId;
        }

        public PerfContext checkpoint(String label) {
            long now = System.nanoTime();
            timings.put(label, (now - lastCheckpoint) / 1_000_000);
            lastCheckpoint = now;
            return this;
        }

        public void complete() {
            long totalMs = (System.nanoTime() - startTime) / 1_000_000;
            timings.put("Total Request", totalMs);

            StringBuilder sb = new StringBuilder();
            sb.append("\n[AI-PERFORMANCE]\n");
            sb.append("  Operation  : ").append(operation).append("\n");
            sb.append("  Request ID : ").append(requestId).append("\n");
            for (Map.Entry<String, Long> entry : timings.entrySet()) {
                sb.append("  ").append(String.format("%-25s", entry.getKey()))
                  .append(": ").append(entry.getValue()).append("ms\n");
            }
            log.info(sb.toString());
        }

        public void fail(String reason) {
            long totalMs = (System.nanoTime() - startTime) / 1_000_000;
            timings.put("Failed After", totalMs);

            StringBuilder sb = new StringBuilder();
            sb.append("\n[AI-PERFORMANCE] FAILED\n");
            sb.append("  Operation  : ").append(operation).append("\n");
            sb.append("  Request ID : ").append(requestId).append("\n");
            sb.append("  Reason     : ").append(reason).append("\n");
            for (Map.Entry<String, Long> entry : timings.entrySet()) {
                sb.append("  ").append(String.format("%-25s", entry.getKey()))
                  .append(": ").append(entry.getValue()).append("ms\n");
            }
            log.warn(sb.toString());
        }
    }
}
