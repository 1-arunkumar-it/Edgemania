package io.edgemania.dto;

import java.util.Map;

public record NodeInstanceResponse(
        String id,
        String typeId,
        String label,
        String category,
        double x,
        double y,
        Map<String, Object> properties,
        String status
) {}
