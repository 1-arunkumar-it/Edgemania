package io.edgemania.model;

import java.util.List;

public record MetricSnapshot(
    int nodes,
    int simulations,
    MetricValue cpu,
    MetricValue memory,
    MetricValue latencyP95,
    int events24h,
    List<Event> events
) {
    public record MetricValue(double value, String unit) {}
    public record Event(String id, String time, String severity, String message) {}
}
