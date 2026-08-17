package com.taskflowpro.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
  @Bean
  OpenAPI taskFlowOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("TaskFlow Pro API")
                .version("1.0.0")
                .description("Workspace-scoped productivity APIs"))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
