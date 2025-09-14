package com.findex.dto.syncjob;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "지수데이터 OpenAPI 연동 요청")
public record IndexDataOpenApiSyncRequest(
    @NotEmpty List<Long> indexInfoIds,

    @NotNull LocalDate baseDateFrom,

    @NotNull LocalDate baseDateTo
) {}
