package io.edgemania;

import io.edgemania.model.Edge;
import io.edgemania.model.Node;
import io.edgemania.model.NodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SampleData {

    private SampleData() {}

    public static final List<NodeType> CATALOG = List.of(
            new NodeType("device", "Device", "source", "secondary",
                    new NodeType.Sockets(List.of(), List.of("data")),
                    List.of(
                            new NodeType.PropertySchema("device_type", "Device Type", "select",
                                    0, 0, 0, 0, List.of("camera", "gateway", "thermostat")),
                            new NodeType.PropertySchema("data_rate", "Data Rate", "number",
                                    1, 100, 1, 50, null))),

            new NodeType("edge", "Edge", "process", "tertiary",
                    new NodeType.Sockets(List.of("data"), List.of("data")),
                    List.of(
                            new NodeType.PropertySchema("cpu_cores", "CPU Cores", "number",
                                    1, 64, 1, 4, null),
                            new NodeType.PropertySchema("ram_gb", "RAM (GB)", "number",
                                    1, 256, 1, 16, null))),

            new NodeType("cloud", "Cloud", "output", "primary",
                    new NodeType.Sockets(List.of("data"), List.of()),
                    List.of(
                            new NodeType.PropertySchema("latency_ms", "Latency (ms)", "number",
                                    1, 500, 1, 45, null)))
    );

    public static List<Node> sampleNodes() {
        List<Node> nodes = new ArrayList<>();
        nodes.add(makeNode("device", "cam-01", 80, 120, Map.of("device_type", "camera", "data_rate", 80)));
        nodes.add(makeNode("edge", "edge-01", 360, 120, Map.of("cpu_cores", 4, "ram_gb", 16)));
        nodes.add(makeNode("cloud", "cloud-01", 620, 120, Map.of("latency_ms", 45)));
        return nodes;
    }

    public static List<Edge> sampleEdges(List<Node> nodes) {
        return List.of(
                new Edge(nodes.get(0).getId(), "data", nodes.get(1).getId(), "data"),
                new Edge(nodes.get(1).getId(), "data", nodes.get(2).getId(), "data")
        );
    }

    private static Node makeNode(String typeId, String label, double x, double y,
                                  Map<String, Object> properties) {
        NodeType type = CATALOG.stream().filter(t -> t.id().equals(typeId)).findFirst().orElseThrow();
        return new Node(typeId, label, type.category(), x, y, properties);
    }
}
