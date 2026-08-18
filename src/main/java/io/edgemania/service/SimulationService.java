package io.edgemania.service;

import io.edgemania.dto.RunSimulationRequest;
import io.edgemania.dto.SimulationRunResponse;
import io.edgemania.exception.ApiException;
import io.edgemania.model.Edge;
import io.edgemania.model.Node;
import io.edgemania.model.SimulationRun;
import io.edgemania.model.NodeType;
import io.edgemania.SampleData;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SimulationService {

    private final Map<String, SimulationRun> runs = new ConcurrentHashMap<>();

    public SimulationRunResponse run(RunSimulationRequest req) {
        var graph = req.graph();
        Map<String, RunSimulationRequest.GraphDtoNode> nodeMap = new LinkedHashMap<>();
        for (var n : graph.nodes()) nodeMap.put(n.id(), n);

        validate(graph.nodes(), nodeMap, graph.edges());

        SimulationRun run = new SimulationRun("sim-" + runs.size(), req.ticks());

        Map<String, Double> values = new HashMap<>();
        Map<String, String> nodeStatus = new HashMap<>();
        List<String> topoOrder = topologicalSort(graph.nodes(), graph.edges());

        for (int tick = 1; tick <= req.ticks(); tick++) {
            for (String nodeId : topoOrder) {
                var n = nodeMap.get(nodeId);
                double val = computeNode(n, tick, values, graph.edges());
                values.put(nodeId, val);
                checkOverload(n, val, nodeStatus);
            }
        }

        List<SimulationRun.NodeOutput> outputs = topoOrder.stream()
                .map(id -> {
                    var n = nodeMap.get(id);
                    String status = nodeStatus.getOrDefault(id, "ok");
                    return new SimulationRun.NodeOutput(id, n.label(), values.getOrDefault(id, 0.0), status);
                })
                .toList();
        run.setNodeOutputs(outputs);
        run.setStatus("COMPLETED");
        run.setFinishedAt(Instant.now());
        runs.put(run.getId(), run);

        return toResponse(run);
    }

    public SimulationRunResponse getRun(String runId) {
        SimulationRun run = runs.get(runId);
        if (run == null) throw new ApiException(HttpStatus.NOT_FOUND, "Run " + runId + " not found");
        return toResponse(run);
    }

    private void validate(List<RunSimulationRequest.GraphDtoNode> nodeList,
                          Map<String, RunSimulationRequest.GraphDtoNode> nodeMap,
                          List<RunSimulationRequest.GraphDtoEdge> edges) {
        // Check for duplicate node IDs
        long distinctIds = nodeList.stream().map(RunSimulationRequest.GraphDtoNode::id).distinct().count();
        if (distinctIds != nodeList.size()) {
            throw new ApiException(HttpStatus.CONFLICT, "Duplicate node ids");
        }
        // Check for duplicate edge references (same from+fromSocket+to+toSocket)
        var edgeKeys = edges.stream()
                .map(e -> e.from() + ":" + e.fromSocket() + "->" + e.to() + ":" + e.toSocket())
                .toList();
        var uniqueEdges = new java.util.HashSet<>(edgeKeys);
        if (uniqueEdges.size() != edgeKeys.size()) {
            throw new ApiException(HttpStatus.CONFLICT, "Duplicate edge references");
        }
        for (var e : edges) {
            if (!nodeMap.containsKey(e.from()))
                throw new ApiException(HttpStatus.CONFLICT,
                        "Edge references unknown source node: " + e.from());
            if (!nodeMap.containsKey(e.to()))
                throw new ApiException(HttpStatus.CONFLICT,
                        "Edge references unknown target node: " + e.to());
            if (e.from().equals(e.to()))
                throw new ApiException(HttpStatus.CONFLICT,
                        "Self-loop detected on node " + e.from());
        }
    }

    private double computeNode(RunSimulationRequest.GraphDtoNode n, int tick,
                                Map<String, Double> values, List<RunSimulationRequest.GraphDtoEdge> edges) {
        String typeId = n.typeId();
        Map<String, Object> props = n.properties() != null ? n.properties() : Map.of();

        List<Double> inputs = edges.stream()
                .filter(e -> e.to().equals(n.id()))
                .map(e -> values.getOrDefault(e.from(), 0.0))
                .toList();

        return switch (typeId) {
            case "device" -> {
                double rate = getDouble(props, "data_rate", 50.0);
                String dtype = getString(props, "device_type", "camera");
                double scale = switch (dtype) {
                    case "gateway" -> 0.8;
                    case "thermostat" -> 0.3;
                    default -> 1.0; // camera
                };
                yield rate * scale * Math.sin(tick * 0.5);
            }
            case "edge" -> {
                double in = inputs.isEmpty() ? 0.0 : inputs.stream().mapToDouble(d -> d).sum();
                double cores = getDouble(props, "cpu_cores", 4.0);
                double ram = Math.max(1.0, getDouble(props, "ram_gb", 16.0));
                yield in * (cores / (ram * 0.1));
            }
            case "cloud" -> inputs.isEmpty() ? 0.0 : inputs.getFirst();
            default -> 0.0;
        };
    }

    private List<String> topologicalSort(List<RunSimulationRequest.GraphDtoNode> nodes,
                                          List<RunSimulationRequest.GraphDtoEdge> edges) {
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        Map<String, List<String>> adj = new LinkedHashMap<>();
        for (var n : nodes) {
            inDegree.put(n.id(), 0);
            adj.put(n.id(), new ArrayList<>());
        }
        for (var e : edges) {
            adj.get(e.from()).add(e.to());
            inDegree.merge(e.to(), 1, Integer::sum);
        }

        Queue<String> queue = new LinkedList<>();
        for (var entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) queue.add(entry.getKey());
        }

        List<String> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            String id = queue.poll();
            order.add(id);
            for (String neighbor : adj.get(id)) {
                int deg = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, deg);
                if (deg == 0) queue.add(neighbor);
            }
        }
        if (order.size() != nodes.size()) {
            throw new ApiException(HttpStatus.CONFLICT, "Graph contains a cycle");
        }
        return order;
    }

    private double getDouble(Map<String, Object> props, String key, double fallback) {
        Object v = props.get(key);
        if (v instanceof Number n) return n.doubleValue();
        return fallback;
    }

    private String getString(Map<String, Object> props, String key, String fallback) {
        Object v = props.get(key);
        return v instanceof String s ? s : fallback;
    }

    private void checkOverload(RunSimulationRequest.GraphDtoNode n, double val,
                                Map<String, String> nodeStatus) {
        if ("edge".equals(n.typeId())) {
            double cores = getDouble(n.properties() != null ? n.properties() : Map.of(), "cpu_cores", 4.0);
            double ram = getDouble(n.properties() != null ? n.properties() : Map.of(), "ram_gb", 16.0);
            double load = Math.abs(val) / (cores * 10);
            if (load > 1.0 || Math.abs(val) > ram * 6.25) {
                nodeStatus.put(n.id(), "overload");
            } else {
                nodeStatus.putIfAbsent(n.id(), "ok");
            }
        }
    }

    private SimulationRunResponse toResponse(SimulationRun run) {
        return new SimulationRunResponse(
                run.getId(), run.getStatus(), run.getTicks(),
                run.getStartedAt(), run.getFinishedAt(),
                run.getNodeOutputs().stream()
                        .map(o -> new SimulationRunResponse.NodeOutputDto(
                                o.nodeId(), o.label(), o.lastValue(), o.status()))
                        .toList());
    }
}
