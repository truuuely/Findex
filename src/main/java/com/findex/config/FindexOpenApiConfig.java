package com.findex.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
    info =
        @Info(
            title = "Findex API 문서",
            description = "Findex 프로젝트의 Swagger API 문서입니다.",
            version = "1.0"),
    servers = {
        @Server(
            url = "http://localhost:8080",
            description = "로컬 서버"
        )
    },
    tags = {
        @Tag(name = "1. 지수 정보 API", description = "지수 정보 관리 API"),
        @Tag(name = "2. 지수 데이터 API", description = "지수 데이터 관리 API"),
        @Tag(name = "3. 연동 작업 API", description = "연동 작업 관리 API"),
        @Tag(name = "4. 자동 연동 설정 API", description = "자동 연동 설정 관리 API")
    }
)
@Configuration
public class FindexOpenApiConfig {}
