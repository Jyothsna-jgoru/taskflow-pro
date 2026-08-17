package com.taskflowpro.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@EnabledIfEnvironmentVariable(named = "RUN_INTEGRATION_TESTS", matches = "true")
class ApiFlowIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @Container
  static GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    registry.add("spring.cache.type", () -> "redis");
    registry.add("app.cache.enabled", () -> "true");
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CacheManager caches;

  @Test
  void fullApiFlowEnforcesRbacAndInvalidatesDashboardCache() throws Exception {
    String admin = register("Admin", "admin@test.local", "Password1!");
    String outsider = register("Outsider", "outsider@test.local", "Password1!");
    JsonNode workspace =
        body(
            mvc.perform(
                    post("/api/workspaces")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Delivery\",\"description\":\"Integration test\"}"))
                .andExpect(status().isCreated())
                .andReturn());
    String workspaceId = workspace.get("id").asText();
    mvc.perform(
            get("/api/workspaces/{id}/tasks", workspaceId)
                .header("Authorization", bearer(outsider)))
        .andExpect(status().isForbidden());

    mvc.perform(
            post("/api/workspaces/{id}/members", workspaceId)
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"outsider@test.local\",\"role\":\"MEMBER\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.role").value("MEMBER"));
    mvc.perform(
            post("/api/workspaces/{id}/projects", workspaceId)
                .header("Authorization", bearer(outsider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Forbidden project\",\"status\":\"ACTIVE\"}"))
        .andExpect(status().isForbidden());

    JsonNode me =
        body(mvc.perform(get("/api/auth/me").header("Authorization", bearer(admin))).andReturn());
    String ownerId = me.get("id").asText();
    JsonNode project =
        body(
            mvc.perform(
                    post("/api/workspaces/{id}/projects", workspaceId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"name\":\"Launch\",\"description\":\"Ship it\",\"status\":\"ACTIVE\",\"ownerId\":\""
                                + ownerId
                                + "\"}"))
                .andExpect(status().isCreated())
                .andReturn());
    JsonNode task =
        body(
            mvc.perform(
                    post("/api/workspaces/{id}/tasks", workspaceId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"projectId\":\""
                                + project.get("id").asText()
                                + "\",\"title\":\"Secure APIs\",\"description\":\"Test boundaries\",\"status\":\"TODO\",\"priority\":\"HIGH\",\"labels\":[\"security\"]}"))
                .andExpect(status().isCreated())
                .andReturn());
    mvc.perform(
            post("/api/workspaces/{wid}/tasks/{tid}/comments", workspaceId, task.get("id").asText())
                .header("Authorization", bearer(outsider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Boundary confirmed\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            get("/api/workspaces/{wid}/tasks/{tid}", workspaceId, task.get("id").asText())
                .header("Authorization", bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.comments[0].body").value("Boundary confirmed"))
        .andExpect(jsonPath("$.activity").isArray());

    mvc.perform(
            get("/api/workspaces/{id}/dashboard", workspaceId)
                .header("Authorization", bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalTasks").value(1));
    String key = "admin@test.local:" + workspaceId;
    assertThat(caches.getCache("dashboard").get(key)).isNotNull();
    mvc.perform(
            get("/api/workspaces/{id}/dashboard", workspaceId)
                .header("Authorization", bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tasksByStatus.TODO").value(1));
    mvc.perform(
            put("/api/workspaces/{wid}/tasks/{tid}", workspaceId, task.get("id").asText())
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"projectId\":\""
                        + project.get("id").asText()
                        + "\",\"title\":\"Secure APIs\",\"description\":\"Done\",\"status\":\"DONE\",\"priority\":\"HIGH\",\"labels\":[\"security\"],\"version\":"
                        + task.get("version").asLong()
                        + "}"))
        .andExpect(status().isOk());
    assertThat(caches.getCache("dashboard").get(key)).isNull();
  }

  private String register(String name, String email, String password) throws Exception {
    JsonNode response =
        body(
            mvc.perform(
                    post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"displayName\":\""
                                + name
                                + "\",\"email\":\""
                                + email
                                + "\",\"password\":\""
                                + password
                                + "\"}"))
                .andExpect(status().isCreated())
                .andReturn());
    return response.get("accessToken").asText();
  }

  private JsonNode body(org.springframework.test.web.servlet.MvcResult result) throws Exception {
    return json.readTree(result.getResponse().getContentAsString());
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }
}
