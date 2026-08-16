package io.edgemania.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateNodeRequest(
        @NotBlank String typeId,
        String label,
        @NotNull @Min(0) double x,
        @NotNull @Min(0) double y,
        Map<String, Object> properties
) {}
