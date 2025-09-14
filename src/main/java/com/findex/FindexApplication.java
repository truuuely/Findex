package com.findex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FindexApplication {

    public static void main(String[] args) {
        SpringApplication.run(FindexApplication.class, args);
    }
}
