package com.tca.peppol.model.response;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ServiceResult<T> {
    private final boolean success;
    private final T data;
    private final int statusCode;
    private final String errorMessage;
    private final String requestId;
    private final Instant timestamp;

    private ServiceResult(boolean success, T data, int statusCode,
                          String errorMessage, String requestId) {
        this.success = success;
        this.data = data;
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
        this.requestId = requestId;
        this.timestamp = Instant.now();
    }

    public static <T> ServiceResult<T> success(T data, int statusCode, String requestId) {
        return new ServiceResult<>(true, data, statusCode, null, requestId);
    }

    public static <T> ServiceResult<T> error(int statusCode, String errorMessage, String requestId) {
        return new ServiceResult<>(false, null, statusCode, errorMessage, requestId);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public int getStatusCode() { return statusCode; }
    public String getErrorMessage() { return errorMessage; }
    public String getRequestId() { return requestId; }
    public Instant getTimestamp() { return timestamp; }

    /**
     * Converts to JSON for HTTP responses
     */
    public String toJson(ObjectMapper objectMapper) throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("requestId", requestId);
        result.put("timestamp", timestamp);

        if (success) {
            result.put("data", data);
        } else {
            result.put("error", errorMessage);
        }

        return objectMapper.writeValueAsString(result);
    }
}