package io.edgemania.controller;

import io.edgemania.dto.*;
import io.edgemania.service.NodeCatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nodes")
public class NodeCatalogController {

    private final NodeCatalogService service;

    public NodeCatalogController(NodeCatalogService service) {
        this.service = service;
    }

    @GetMapping
    public NodeListResponse list() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    public NodeInstanceResponse get(@PathVariable String id) {
        return service.getInstance(id);
    }

    @PostMapping
    public ResponseEntity<NodeInstanceResponse> create(@Valid @RequestBody CreateNodeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PostMapping("/sample")
    public ResponseEntity<List<NodeInstanceResponse>> createSample() {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSample());
    }

    @PutMapping("/{id}")
    public NodeInstanceResponse update(@PathVariable String id,
                                        @Valid @RequestBody UpdateNodeRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
