package com.findex.dto.syncjob;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "지수데이터 OpenAPI 연동 요청")
public record IndexDataOpenApiSyncRequest(
    @NotNull Long indexInfoId,
    @NotNull LocalDate from,
    @NotNull LocalDate to
) {}