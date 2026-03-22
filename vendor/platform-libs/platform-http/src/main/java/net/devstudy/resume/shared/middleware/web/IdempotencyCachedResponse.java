package net.devstudy.resume.shared.middleware.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record IdempotencyCachedResponse(int status, Map<String, List<String>> headers, byte[] body) {

    public IdempotencyCachedResponse {
        headers = copyHeaders(headers);
        body = body == null ? new byte[0] : body.clone();
    }

    private static Map<String, List<String>> copyHeaders(Map<String, List<String>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            String headerName = entry.getKey();
            if (headerName == null || headerName.isBlank()) {
                continue;
            }
            List<String> values = entry.getValue() == null ? List.of() : new ArrayList<>(entry.getValue());
            copy.put(headerName, values);
        }
        return copy;
    }
}
