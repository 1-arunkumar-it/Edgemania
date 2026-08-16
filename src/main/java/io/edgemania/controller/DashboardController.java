package io.edgemania.controller;

import io.edgemania.dto.MetricHistoryResponse;
import io.edgemania.exception.ApiException;
import io.edgemania.model.MetricSnapshot;
import io.edgemania.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Set<String> VALID_WINDOWS = Set.of("5m", "15m", "1h", "24h");

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/metrics")
    public MetricSnapshot metrics() {
        return service.getSnapshot();
    }

    @GetMapping("/metrics/history")
    public MetricHistoryResponse metricsHistory(
            @RequestParam(defaultValue = "5m") String window) {
        if (!VALID_WINDOWS.contains(window)) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Invalid window: " + window + ". Accepted: 5m, 15m, 1h, 24h");
        }
        return service.getHistory(window);
    }
}
