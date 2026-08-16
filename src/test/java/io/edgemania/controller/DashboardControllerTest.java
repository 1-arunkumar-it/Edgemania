package io.edgemania.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void metricsReturns200WithCorrectShape() throws Exception {
        mvc.perform(get("/api/dashboard/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes").isNumber())
                .andExpect(jsonPath("$.simulations").isNumber())
                .andExpect(jsonPath("$.cpu.value").isNumber())
                .andExpect(jsonPath("$.cpu.unit").value("%"))
                .andExpect(jsonPath("$.memory.value").isNumber())
                .andExpect(jsonPath("$.memory.unit").value("%"))
                .andExpect(jsonPath("$.latencyP95.value").isNumber())
                .andExpect(jsonPath("$.latencyP95.unit").value("ms"))
                .andExpect(jsonPath("$.events24h").isNumber())
                .andExpect(jsonPath("$.events").isArray());
    }

    @Test
    void historyDefaultWindowReturns200() throws Exception {
        mvc.perform(get("/api/dashboard/metrics/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.window").value("5m"))
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points.length()").isNumber())
                .andExpect(jsonPath("$.points[0].t").isNumber())
                .andExpect(jsonPath("$.points[0].cpu").isNumber())
                .andExpect(jsonPath("$.points[0].memory").isNumber())
                .andExpect(jsonPath("$.points[0].latency").isNumber());
    }

    @Test
    void historyExplicitWindowReturns200() throws Exception {
        mvc.perform(get("/api/dashboard/metrics/history?window=15m"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.window").value("15m"))
                .andExpect(jsonPath("$.points").isArray());
    }

    @Test
    void historyInvalidWindowReturns400() throws Exception {
        mvc.perform(get("/api/dashboard/metrics/history?window=99m"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
