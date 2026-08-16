package io.edgemania.model;

import java.util.List;

public record NodeType(
        String id,
        String label,
        String category,
        String color,
        Sockets sockets,
        List<PropertySchema> properties
) {
    public record Sockets(List<String> inputs, List<String> outputs) {}

    public record PropertySchema(
            String key,
            String label,
            String type,
            double min,
            double max,
            double step,
            double defaultVal,
            List<String> options
    ) {}
}
