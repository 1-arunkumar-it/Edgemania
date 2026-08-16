package io.edgemania.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SimulationControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void runHappyPathReturns200WithCompleted() throws Exception {
        mvc.perform(post("/api/simulations/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "graph": {
                                    "nodes": [
                                      {"id":"n1","typeId":"device","label":"cam","x":0,"y":0,"properties":{"device_type":"camera","data_rate":80}},
                                      {"id":"n2","typeId":"cloud","label":"cld","x":200,"y":0,"properties":{"latency_ms":45}}
                                    ],
                                    "edges": [
                                      {"id":"e1","from":"n1","fromSocket":"data","to":"n2","toSocket":"data"}
                                    ]
                                  },
                                  "ticks": 10,
                                  "tickMs": 100
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.ticks").value(10))
                .andExpect(jsonPath("$.nodeOutputs.length()").value(2))
                .andExpect(jsonPath("$.nodeOutputs[0].nodeId").value("n1"))
                .andExpect(jsonPath("$.nodeOutputs[0].label").value("cam"));
    }

    @Test
    void runUnknownNodeIdReturns409() throws Exception {
        mvc.perform(post("/api/simulations/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "graph": {
                                    "nodes": [{"id":"n1","typeId":"device","label":"d","x":0,"y":0,"properties":{}}],
                                    "edges": [{"id":"e1","from":"n1","fromSocket":"data","to":"unknown","toSocket":"data"}]
                                  },
                                  "ticks": 5,
                                  "tickMs": 100
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void runTicksZeroReturns400() throws Exception {
        mvc.perform(post("/api/simulations/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "graph": {"nodes":[],"edges":[]},
                                  "ticks": 0,
                                  "tickMs": 100
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void runEmptyGraphValidReturns200() throws Exception {
        mvc.perform(post("/api/simulations/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "graph": {"nodes":[],"edges":[]},
                                  "ticks": 1,
                                  "tickMs": 100
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.nodeOutputs.length()").value(0));
    }

    @Test
    void getRunNotFoundReturns404() throws Exception {
        mvc.perform(get("/api/simulations/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}
