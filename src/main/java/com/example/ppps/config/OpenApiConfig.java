// REVIEW: full code review requested

package com.example.ppps.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "PPPS API", version = "1.0", description = "Personal P2P Payment Service API"))
public class OpenApiConfig {
}