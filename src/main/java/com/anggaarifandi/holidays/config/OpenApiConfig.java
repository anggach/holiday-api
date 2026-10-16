package com.anggaarifandi.holidays.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI holidaysOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Holidays API")
						.description("Public holidays API, with optional Ollama agent")
						.version("v1"))
				.components(new Components().addSecuritySchemes("ApiKeyAuth",
						new SecurityScheme()
								.type(SecurityScheme.Type.APIKEY)
								.in(SecurityScheme.In.HEADER)
								.name("X-API-Key")));
	}

}
