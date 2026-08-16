package io.edgemania.controller;

import io.edgemania.dto.RunSimulationRequest;
import io.edgemania.dto.SimulationRunResponse;
import io.edgemania.service.SimulationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulations")
public class SimulationController {

    private final SimulationService service;

    public SimulationController(SimulationService service) {
        this.service = service;
    }

    @PostMapping("/run")
    public SimulationRunResponse run(@Valid @RequestBody RunSimulationRequest req) {
        return service.run(req);
    }

    @GetMapping("/{runId}")
    public SimulationRunResponse getRun(@PathVariable String runId) {
        return service.getRun(runId);
    }
}
