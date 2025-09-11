package com.findex.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "findex.openapi")
public record OpenApiProperties(@NotBlank String baseUrl, String serviceKey) {

}
