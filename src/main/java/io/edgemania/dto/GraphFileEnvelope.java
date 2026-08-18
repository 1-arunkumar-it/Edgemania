package io.edgemania.dto;

import java.util.List;

public record GraphFileEnvelope(
        String format,
        int version,
        String name,
        String savedAt,
        RunSimulationRequest.GraphDto graph
) {
    public static GraphFileEnvelope of(String name, String savedAt,
                                       RunSimulationRequest.GraphDto graph) {
        return new GraphFileEnvelope("edgemania", 1, name, savedAt, graph);
    }
}
