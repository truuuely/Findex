package com.findex.dto.syncjob;

public record OpenApiIndexInfoItem(
    String idxCsf,
    String idxNm,
    Integer epyItmsCnt,
    String basPntm,
    Integer basIdx
) {}
