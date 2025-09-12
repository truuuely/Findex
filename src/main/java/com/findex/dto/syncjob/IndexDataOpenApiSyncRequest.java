package com.findex.dto.syncjob;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "지수데이터 OpenAPI 연동 요청")
public record IndexDataOpenApiSyncRequest(
    @NotNull LocalDate from,
    @NotNull LocalDate to,
    @Schema(description = "지수명(선택). 비우면 DB에 있는 모든 지수 대상") String indexName
) {}