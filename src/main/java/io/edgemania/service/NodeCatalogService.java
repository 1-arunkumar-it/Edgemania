package io.edgemania.service;

import io.edgemania.SampleData;
import io.edgemania.dto.*;
import io.edgemania.exception.ApiException;
import io.edgemania.model.Node;
import io.edgemania.model.NodeType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class NodeCatalogService {

    private final List<NodeType> catalog = SampleData.CATALOG;
    private final Map<String, Node> instances = new ConcurrentHashMap<>();

    public NodeListResponse listAll() {
        List<NodeTypeResponse> types = catalog.stream().map(this::toTypeResponse).toList();
        List<NodeInstanceResponse> inst = instances.values().stream().map(this::toInstanceResponse).toList();
        return new NodeListResponse(types, inst);
    }

    public NodeInstanceResponse getInstance(String id) {
        Node node = instances.get(id);
        if (node == null) throw new ApiException(HttpStatus.NOT_FOUND, "Node " + id + " not found");
        return toInstanceResponse(node);
    }

    public NodeInstanceResponse create(CreateNodeRequest req) {
        NodeType type = catalog.stream()
                .filter(t -> t.id().equals(req.typeId()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "Unknown node type: " + req.typeId()));

        Map<String, Object> props = new HashMap<>();
        for (NodeType.PropertySchema ps : type.properties()) {
            props.put(ps.key(), req.properties() != null && req.properties().containsKey(ps.key())
                    ? req.properties().get(ps.key())
                    : ps.defaultVal());
        }
        if (req.properties() != null) {
            req.properties().forEach(props::putIfAbsent);
        }

        String label = req.label() != null ? req.label() : req.typeId() + "-" + instances.size();
        Node node = new Node(req.typeId(), label, type.category(), req.x(), req.y(), props);
        instances.put(node.getId(), node);
        return toInstanceResponse(node);
    }

    public List<NodeInstanceResponse> createSample() {
        List<Node> nodes = SampleData.sampleNodes();
        List<NodeInstanceResponse> result = new ArrayList<>();
        for (Node node : nodes) {
            instances.put(node.getId(), node);
            result.add(toInstanceResponse(node));
        }
        return result;
    }

    public NodeInstanceResponse update(String id, UpdateNodeRequest req) {
        Node node = instances.get(id);
        if (node == null) throw new ApiException(HttpStatus.NOT_FOUND, "Node " + id + " not found");
        if (req.properties() != null) node.setProperties(req.properties());
        return toInstanceResponse(node);
    }

    public void delete(String id) {
        if (instances.remove(id) == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Node " + id + " not found");
        }
    }

    public void deleteAll() {
        instances.clear();
    }

    public Map<String, Node> getInstances() {
        return instances;
    }

    private NodeTypeResponse toTypeResponse(NodeType t) {
        return new NodeTypeResponse(
                t.id(), t.label(), t.category(), t.color(),
                new NodeTypeResponse.SocketsResponse(
                        t.sockets().inputs(), t.sockets().outputs()),
                t.properties().stream()
                        .map(p -> new NodeTypeResponse.PropertySchemaResponse(
                                p.key(), p.label(), p.type(),
                                p.min(), p.max(), p.step(), p.defaultVal(), p.options()))
                        .toList());
    }

    private NodeInstanceResponse toInstanceResponse(Node n) {
        return new NodeInstanceResponse(
                n.getId(), n.getTypeId(), n.getLabel(), n.getCategory(),
                n.getX(), n.getY(), n.getProperties(), n.getStatus());
    }
}
