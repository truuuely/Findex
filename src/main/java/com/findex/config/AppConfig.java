package com.findex.config;


import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OpenApiProperties.class)
public class AppConfig {

  @Bean
  RestClient openApiRestClient(OpenApiProperties props) {
    return RestClient.builder()
        .baseUrl(props.baseUrl())
        .build();
  }
}
