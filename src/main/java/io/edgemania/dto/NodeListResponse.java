package io.edgemania.dto;

import java.util.List;

public record NodeListResponse(
        List<NodeTypeResponse> types,
        List<NodeInstanceResponse> instances
) {}
