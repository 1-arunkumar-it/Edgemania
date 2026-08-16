package io.edgemania.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandlerTest.TestController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiException_returnsStatusAndEnvelope() throws Exception {
        mockMvc.perform(get("/api/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Node abc not found"))
                .andExpect(jsonPath("$.path").value("/api/test/not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void validationFailure_returns400WithFirstMessage() throws Exception {
        mockMvc.perform(post("/api/test/validate")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("name is required"))
                .andExpect(jsonPath("$.path").value("/api/test/validate"));
    }

    @Test
    void unreadableBody_returns400() throws Exception {
        mockMvc.perform(post("/api/test/validate")
                        .contentType("application/json")
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void unexpectedError_returns500Generic() throws Exception {
        mockMvc.perform(get("/api/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Internal Server Error"))
                .andExpect(jsonPath("$.path").value("/api/test/boom"));
    }

    @RestController
    public static class TestController {

        @GetMapping("/api/test/not-found")
        public void notFound() {
            throw new ApiException(HttpStatus.NOT_FOUND, "Node abc not found");
        }

        @PostMapping("/api/test/validate")
        public void validate(@Valid @RequestBody TestRequest body) {
        }

        @GetMapping("/api/test/boom")
        public void boom() {
            throw new IllegalStateException("boom");
        }
    }

    public record TestRequest(@NotBlank(message = "name is required") String name) {
    }
}
