package io.edgemania.dto;

import java.time.Instant;
import java.util.List;

public record SimulationRunResponse(
        String runId,
        String status,
        int ticks,
        Instant startedAt,
        Instant finishedAt,
        List<NodeOutputDto> nodeOutputs
) {
    public record NodeOutputDto(String nodeId, String label, double lastValue, String status) {}
}
