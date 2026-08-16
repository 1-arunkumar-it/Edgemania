package io.edgemania.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class SimulationRun {
    private final String id;
    private String label;
    private String status;
    private int ticks;
    private Instant startedAt;
    private Instant finishedAt;
    private List<NodeOutput> nodeOutputs;

    public SimulationRun(String label, int ticks) {
        this.id = UUID.randomUUID().toString();
        this.label = label;
        this.status = "RUNNING";
        this.ticks = ticks;
        this.startedAt = Instant.now();
        this.nodeOutputs = List.of();
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTicks() { return ticks; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
    public List<NodeOutput> getNodeOutputs() { return nodeOutputs; }
    public void setNodeOutputs(List<NodeOutput> nodeOutputs) { this.nodeOutputs = nodeOutputs; }

    public record NodeOutput(String nodeId, String label, double lastValue, String status) {}
}
