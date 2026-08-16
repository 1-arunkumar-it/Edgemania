package io.edgemania.model;

public record MetricPoint(long t, double cpu, double memory, double latency) {}
