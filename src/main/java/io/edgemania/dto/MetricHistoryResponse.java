package io.edgemania.dto;

import io.edgemania.model.MetricPoint;
import java.util.List;

public record MetricHistoryResponse(String window, List<MetricPoint> points) {}
