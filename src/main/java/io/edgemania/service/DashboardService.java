package io.edgemania.service;

import io.edgemania.dto.MetricHistoryResponse;
import io.edgemania.model.MetricPoint;
import io.edgemania.model.MetricSnapshot;
import io.edgemania.model.MetricSnapshot.Event;
import io.edgemania.model.MetricSnapshot.MetricValue;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class DashboardService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_HISTORY = 1000;
    private static final String[] EVENT_MESSAGES = {
        "Pipeline cam-01 completed successfully",
        "Edge node edge-01 CPU spike detected",
        "Cloud sync latency recovered to normal",
        "Device gateway-01 reconnected after timeout",
        "Security scan passed on all nodes",
        "Memory pressure alert on edge-01",
        "Data rate throttled on camera device",
        "Simulation run completed with 3 nodes",
        "Auth token refreshed for cloud endpoint",
        "Packet loss detected on edge link"
    };
    private static final String[] SEVERITIES = {"info", "info", "info", "warning", "warning", "critical"};

    private final Random rng = new Random();
    private final ConcurrentLinkedDeque<MetricPoint> history = new ConcurrentLinkedDeque<>();
    private final List<Event> events = new CopyOnWriteArrayList<>();
    private final NodeCatalogService nodeCatalogService;
    private final SimulationService simulationService;
    private double cpu = 45.0;
    private double memory = 62.0;
    private double latency = 18.0;
    private int events24h = 0;

    public DashboardService(NodeCatalogService nodeCatalogService, SimulationService simulationService) {
        this.nodeCatalogService = nodeCatalogService;
        this.simulationService = simulationService;
    }

    public MetricSnapshot getSnapshot() {
        drift();
        generateEvent();

        return new MetricSnapshot(
            nodeCatalogService.getInstances().size(),
            simulationService.getCompletedRunCount(),
            new MetricValue(round(cpu), "%"),
            new MetricValue(round(memory), "%"),
            new MetricValue(round(latency), "ms"),
            events24h,
            List.copyOf(events)
        );
    }

    public MetricHistoryResponse getHistory(String window) {
        ensureHistory();
        List<MetricPoint> points = history.stream().toList();
        return new MetricHistoryResponse(window, points);
    }

    private void drift() {
        cpu = clamp(cpu + (rng.nextDouble() - 0.48) * 4.0, 25.0, 90.0);
        memory = clamp(memory + (rng.nextDouble() - 0.50) * 1.5, 40.0, 92.0);
        latency = clamp(latency + (rng.nextDouble() - 0.49) * 3.0, 8.0, 55.0);

        MetricPoint point = new MetricPoint(
            System.currentTimeMillis(),
            round(cpu), round(memory), round(latency)
        );
        history.addLast(point);
        while (history.size() > MAX_HISTORY) history.removeFirst();
    }

    private void ensureHistory() {
        if (!history.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (int i = 60; i >= 0; i--) {
            double c = clamp(45 + (rng.nextDouble() - 0.5) * 20, 25, 90);
            double m = clamp(62 + (rng.nextDouble() - 0.5) * 8, 40, 92);
            double l = clamp(18 + (rng.nextDouble() - 0.5) * 10, 8, 55);
            history.addLast(new MetricPoint(now - i * 5000L, round(c), round(m), round(l)));
        }
    }

    private void generateEvent() {
        if (rng.nextInt(3) != 0) return;
        String severity = SEVERITIES[rng.nextInt(SEVERITIES.length)];
        String message = EVENT_MESSAGES[rng.nextInt(EVENT_MESSAGES.length)];
        String time = LocalTime.now().format(TIME_FMT);
        String id = "e" + UUID.randomUUID().toString().substring(0, 6);
        events.addFirst(new Event(id, time, severity, message));
        while (events.size() > 20) events.removeLast();
        events24h++;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
