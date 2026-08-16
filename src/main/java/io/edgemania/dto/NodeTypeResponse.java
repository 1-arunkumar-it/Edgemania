package io.edgemania.dto;

import java.util.List;

public record NodeTypeResponse(
        String id,
        String label,
        String category,
        String color,
        SocketsResponse sockets,
        List<PropertySchemaResponse> properties
) {
    public record SocketsResponse(List<String> inputs, List<String> outputs) {}
    public record PropertySchemaResponse(
            String key, String label, String type,
            double min, double max, double step, double defaultVal,
            List<String> options
    ) {}
}
