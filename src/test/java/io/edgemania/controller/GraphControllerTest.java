package io.edgemania.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GraphControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    private String validGraphJson() {
        return """
                {
                  "name": "test-graph",
                  "graph": {
                    "nodes": [
                      {"id":"n1","typeId":"device","label":"cam","x":0,"y":0,"properties":{"device_type":"camera","data_rate":80}},
                      {"id":"n2","typeId":"cloud","label":"cld","x":200,"y":0,"properties":{"latency_ms":45}}
                    ],
                    "edges": [
                      {"id":"e1","from":"n1","fromSocket":"data","to":"n2","toSocket":"data"}
                    ]
                  }
                }
                """;
    }

    private MockMultipartFile emFile(String name, String content) {
        return new MockMultipartFile(
                "file", name + ".em", "application/x-edgemania",
                content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void exportHappyPathReturnsEmFile() throws Exception {
        MvcResult result = mvc.perform(post("/api/graphs/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validGraphJson()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/x-edgemania"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("test-graph.em")))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("\"format\" : \"edgemania\""));
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("\"version\" : 1"));
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("\"name\" : \"test-graph\""));
    }

    @Test
    void exportBlankNameReturns400() throws Exception {
        mvc.perform(post("/api/graphs/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","graph":{"nodes":[],"edges":[]}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importValidEmRoundTrip() throws Exception {
        String emContent = """
                {
                  "format": "edgemania",
                  "version": 1,
                  "name": "test-graph",
                  "savedAt": "2026-08-16T04:12:55Z",
                  "graph": {
                    "nodes": [
                      {"id":"n1","typeId":"device","label":"cam","x":0,"y":0,"properties":{"device_type":"camera","data_rate":80}},
                      {"id":"n2","typeId":"cloud","label":"cld","x":200,"y":0,"properties":{"latency_ms":45}}
                    ],
                    "edges": [
                      {"id":"e1","from":"n1","fromSocket":"data","to":"n2","toSocket":"data"}
                    ]
                  }
                }
                """;

        MockMultipartFile file = emFile("test-graph", emContent);

        mvc.perform(multipart("/api/graphs/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test-graph"))
                .andExpect(jsonPath("$.savedAt").value("2026-08-16T04:12:55Z"))
                .andExpect(jsonPath("$.graph.nodes.length()").value(2))
                .andExpect(jsonPath("$.graph.edges.length()").value(1));
    }

    @Test
    void importWrongExtensionReturns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.json", "application/json",
                "{}".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/graphs/import").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importMalformedJsonReturns400() throws Exception {
        MockMultipartFile file = emFile("bad", "not json at all");

        mvc.perform(multipart("/api/graphs/import").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importWrongFormatReturns400() throws Exception {
        String badFormat = """
                {
                  "format": "wrong",
                  "version": 1,
                  "name": "test",
                  "savedAt": "2026-08-16T04:12:55Z",
                  "graph": {"nodes":[],"edges":[]}
                }
                """;
        MockMultipartFile file = emFile("bad-format", badFormat);

        mvc.perform(multipart("/api/graphs/import").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importWrongVersionReturns400() throws Exception {
        String badVersion = """
                {
                  "format": "edgemania",
                  "version": 2,
                  "name": "test",
                  "savedAt": "2026-08-16T04:12:55Z",
                  "graph": {"nodes":[],"edges":[]}
                }
                """;
        MockMultipartFile file = emFile("bad-version", badVersion);

        mvc.perform(multipart("/api/graphs/import").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importDuplicateNodeIdsReturns400() throws Exception {
        String dupNodes = """
                {
                  "format": "edgemania",
                  "version": 1,
                  "name": "test",
                  "savedAt": "2026-08-16T04:12:55Z",
                  "graph": {
                    "nodes": [
                      {"id":"n1","typeId":"device","label":"a","x":0,"y":0,"properties":{}},
                      {"id":"n1","typeId":"device","label":"b","x":10,"y":0,"properties":{}}
                    ],
                    "edges": []
                  }
                }
                """;
        MockMultipartFile file = emFile("dup-nodes", dupNodes);

        mvc.perform(multipart("/api/graphs/import").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importEdgeToMissingNodeReturns400() throws Exception {
        String badEdge = """
                {
                  "format": "edgemania",
                  "version": 1,
                  "name": "test",
                  "savedAt": "2026-08-16T04:12:55Z",
                  "graph": {
                    "nodes": [
                      {"id":"n1","typeId":"device","label":"a","x":0,"y":0,"properties":{}}
                    ],
                    "edges": [
                      {"id":"e1","from":"n1","fromSocket":"data","to":"n99","toSocket":"data"}
                    ]
                  }
                }
                """;
        MockMultipartFile file = emFile("bad-edge", badEdge);

        mvc.perform(multipart("/api/graphs/import").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importUnknownTypeIdReturns400() throws Exception {
        String badType = """
                {
                  "format": "edgemania",
                  "version": 1,
                  "name": "test",
                  "savedAt": "2026-08-16T04:12:55Z",
                  "graph": {
                    "nodes": [
                      {"id":"n1","typeId":"nope","label":"a","x":0,"y":0,"properties":{}}
                    ],
                    "edges": []
                  }
                }
                """;
        MockMultipartFile file = emFile("bad-type", badType);

        mvc.perform(multipart("/api/graphs/import").file(file))
                .andExpect(status().isBadRequest());
    }
}
