package com.teamhub.teamhub_backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * SSE event payload for streaming AI responses.
 * Used by React frontends consuming EventSource / fetch with ReadableStream.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiStreamEvent(
        String type,       // "chunk", "complete", "error"
        String content,    // text chunk or final content
        String requestId,  // for correlation
        String error       // only for error events
) {
    public static AiStreamEvent chunk(String content, String requestId) {
        return new AiStreamEvent("chunk", content, requestId, null);
    }

    public static AiStreamEvent complete(String content, String requestId) {
        return new AiStreamEvent("complete", content, requestId, null);
    }

    public static AiStreamEvent error(String error, String requestId) {
        return new AiStreamEvent("error", null, requestId, error);
    }
}
