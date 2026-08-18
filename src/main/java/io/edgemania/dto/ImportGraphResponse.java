package io.edgemania.dto;

public record ImportGraphResponse(
        String name,
        String savedAt,
        RunSimulationRequest.GraphDto graph
) {}
