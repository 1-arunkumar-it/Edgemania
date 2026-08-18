package io.edgemania.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RunSimulationRequest(
        @Valid @NotNull GraphDto graph,
        @Min(1) @Max(10000) int ticks,
        @Min(1) @Max(1000) int tickMs
) {
    public record GraphDto(
            @NotNull List<GraphDtoNode> nodes,
            @NotNull List<GraphDtoEdge> edges
    ) {}
    public record GraphDtoNode(
            String id, String typeId, String label,
            double x, double y,
            java.util.Map<String, Object> properties
    ) {}
    public record GraphDtoEdge(
            String id, String from, String fromSocket, String to, String toSocket
    ) {}
}
