package com.hogu.am_i_hogu.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpenApiConfig.OpenApiProperties.class)
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI(OpenApiProperties properties) {
        Info info = new Info()
                .title(properties.title())
                .version(properties.version());

        Server server = new Server().url(properties.serverUri());

        return new OpenAPI()
                .info(info)
                .addServersItem(server)
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ));
    }

    @ConfigurationProperties(prefix = "app.openapi")
    public record OpenApiProperties(
            String title,
            String version,
            String serverUri
    ) {
    }
}
