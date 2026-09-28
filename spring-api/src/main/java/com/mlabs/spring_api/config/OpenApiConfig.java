package com.mlabs.spring_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI employeePolicyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Employee Policy and Reimbursement Triage API")
                        .version("2.1.0")
                        .description("Assessment API for policy questions and reimbursement batch review. " +
                                "Caller identity is simulated by X-Caller-Id for this local exercise."));
    }
}
