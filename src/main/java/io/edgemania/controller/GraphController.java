package io.edgemania.controller;

import io.edgemania.dto.*;
import io.edgemania.service.GraphFileService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/graphs")
public class GraphController {

    private final GraphFileService graphFileService;

    public GraphController(GraphFileService graphFileService) {
        this.graphFileService = graphFileService;
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> exportGraph(@Valid @RequestBody ExportGraphRequest req) throws Exception {
        byte[] emBytes = graphFileService.exportToEm(req.name(), req.graph());

        String safeName = graphFileService.sanitizeFilename(req.name()) + ".em";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/x-edgemania"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(safeName).build());
        headers.setContentLength(emBytes.length);

        return new ResponseEntity<>(emBytes, headers, HttpStatus.OK);
    }

    @PostMapping("/import")
    public ImportGraphResponse importGraph(@RequestParam("file") MultipartFile file) {
        return graphFileService.importFromEm(file);
    }
}
