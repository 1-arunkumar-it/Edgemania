package io.edgemania.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NodeCatalogControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    String extractId(ResultActions result) throws Exception {
        JsonNode json = objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
        return json.get("id").asText();
    }

    @Test
    void listTypesReturnsCatalog() throws Exception {
        mvc.perform(get("/api/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.types.length()").value(3))
                .andExpect(jsonPath("$.types[0].id").value("device"));
    }

    @Test
    void createNodeReturns201() throws Exception {
        mvc.perform(post("/api/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"typeId":"device","label":"s1","x":10,"y":20,"properties":{"device_type":"camera","data_rate":60}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.typeId").value("device"))
                .andExpect(jsonPath("$.label").value("s1"))
                .andExpect(jsonPath("$.x").value(10))
                .andExpect(jsonPath("$.y").value(20));
    }

    @Test
    void createNodeInvalidTypeReturns400() throws Exception {
        mvc.perform(post("/api/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"typeId":"nope","x":0,"y":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void createSampleReturns201() throws Exception {
        mvc.perform(post("/api/nodes/sample"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void getSingleNode() throws Exception {
        var result = mvc.perform(post("/api/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"typeId":"device","label":"x","x":0,"y":0}
                                """));
        String id = extractId(result);

        mvc.perform(get("/api/nodes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void updateNodeReturns200() throws Exception {
        var result = mvc.perform(post("/api/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"typeId":"device","label":"u","x":0,"y":0,"properties":{"device_type":"gateway","data_rate":30}}
                                """));
        String id = extractId(result);

        mvc.perform(put("/api/nodes/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"properties":{"data_rate":80}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties.data_rate").value(80));
    }

    @Test
    void deleteNodeReturns204() throws Exception {
        var result = mvc.perform(post("/api/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"typeId":"device","label":"d","x":0,"y":0}
                                """));
        String id = extractId(result);

        mvc.perform(delete("/api/nodes/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUnknownReturns404() throws Exception {
        mvc.perform(delete("/api/nodes/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}
