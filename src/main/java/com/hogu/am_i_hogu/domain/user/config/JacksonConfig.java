package com.hogu.am_i_hogu.domain.user.config;

import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class JacksonConfig {
    @Bean
    Jackson2ObjectMapperBuilder objectMapperBuilder() {
        Jackson2ObjectMapperBuilder builder =
                new Jackson2ObjectMapperBuilder();
        builder.modulesToInstall(new JsonNullableModule());

        return builder;
    }
}
