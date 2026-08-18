package io.edgemania.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.edgemania.dto.*;
import io.edgemania.exception.ApiException;
import io.edgemania.model.NodeType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GraphFileService {

    private final ObjectMapper objectMapper;
    private final NodeCatalogService catalogService;

    public GraphFileService(ObjectMapper objectMapper, NodeCatalogService catalogService) {
        this.objectMapper = objectMapper;
        this.catalogService = catalogService;
    }

    public byte[] exportToEm(String name, RunSimulationRequest.GraphDto graph) throws IOException {
        String sanitizedName = sanitizeFilename(name);
        String savedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();
        GraphFileEnvelope envelope = GraphFileEnvelope.of(sanitizedName, savedAt, graph);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(envelope);
    }

    public String sanitizeFilename(String name) {
        String sanitized = name.replaceAll("[^A-Za-z0-9 _-]", "").replaceAll("[/\\\\]", "");
        if (sanitized.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Filename must contain at least one valid character");
        }
        if (sanitized.length() > 64) {
            sanitized = sanitized.substring(0, 64);
        }
        return sanitized.trim();
    }

    public ImportGraphResponse importFromEm(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".em")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File must have a .em extension");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(file.getBytes());
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Malformed JSON in .em file");
        }

        if (!root.has("format") || !"edgemania".equals(root.get("format").asText())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid .em file: missing or wrong format");
        }
        if (!root.has("version") || root.get("version").asInt() != 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid .em file: unsupported version (expected 1)");
        }

        RunSimulationRequest.GraphDto graph;
        try {
            graph = objectMapper.treeToValue(root.get("graph"), RunSimulationRequest.GraphDto.class);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Malformed graph data in .em file");
        }

        validateGraph(graph);

        String name = root.has("name") ? root.get("name").asText("") : "";
        String savedAt = root.has("savedAt") ? root.get("savedAt").asText("") : "";

        return new ImportGraphResponse(name, savedAt, graph);
    }

    public void validateGraph(RunSimulationRequest.GraphDto graph) {
        List<RunSimulationRequest.GraphDtoNode> nodes = graph.nodes();
        List<RunSimulationRequest.GraphDtoEdge> edges = graph.edges();

        Set<String> nodeIds = nodes.stream()
                .map(RunSimulationRequest.GraphDtoNode::id)
                .collect(Collectors.toSet());

        List<String> nodeIdsList = nodes.stream()
                .map(RunSimulationRequest.GraphDtoNode::id)
                .toList();
        Set<String> dupNodeIds = nodeIdsList.stream()
                .filter(id -> Collections.frequency(nodeIdsList, id) > 1)
                .collect(Collectors.toSet());
        if (!dupNodeIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Duplicate node IDs: " + String.join(", ", dupNodeIds));
        }

        List<String> edgeIdsList = edges.stream()
                .map(RunSimulationRequest.GraphDtoEdge::id)
                .toList();
        Set<String> dupEdgeIds = edgeIdsList.stream()
                .filter(id -> Collections.frequency(edgeIdsList, id) > 1)
                .collect(Collectors.toSet());
        if (!dupEdgeIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Duplicate edge IDs: " + String.join(", ", dupEdgeIds));
        }

        Map<String, NodeType> typeMap = catalogService.getCatalog().stream()
                .collect(Collectors.toMap(NodeType::id, t -> t));

        for (RunSimulationRequest.GraphDtoNode node : nodes) {
            if (!typeMap.containsKey(node.typeId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Unknown node type: " + node.typeId());
            }
        }

        for (RunSimulationRequest.GraphDtoEdge edge : edges) {
            if (!nodeIds.contains(edge.from())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Edge references unknown node: " + edge.from());
            }
            if (!nodeIds.contains(edge.to())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Edge references unknown node: " + edge.to());
            }

            String fromTypeId = nodes.stream()
                    .filter(n -> n.id().equals(edge.from()))
                    .findFirst().map(RunSimulationRequest.GraphDtoNode::typeId).orElse(null);
            String toTypeId = nodes.stream()
                    .filter(n -> n.id().equals(edge.to()))
                    .findFirst().map(RunSimulationRequest.GraphDtoNode::typeId).orElse(null);

            if (fromTypeId != null) {
                NodeType fromType = typeMap.get(fromTypeId);
                if (fromType != null && !fromType.sockets().outputs().contains(edge.fromSocket())) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "Invalid output socket '" + edge.fromSocket() + "' on type '" + fromTypeId + "'");
                }
            }
            if (toTypeId != null) {
                NodeType toType = typeMap.get(toTypeId);
                if (toType != null && !toType.sockets().inputs().contains(edge.toSocket())) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "Invalid input socket '" + edge.toSocket() + "' on type '" + toTypeId + "'");
                }
            }
        }
    }
}
