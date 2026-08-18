package io.edgemania.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record ExportGraphRequest(
        @NotBlank String name,
        @Valid RunSimulationRequest.GraphDto graph
) {}
