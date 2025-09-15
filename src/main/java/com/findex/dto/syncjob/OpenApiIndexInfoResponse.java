package com.findex.dto.syncjob;

import java.util.List;

public record OpenApiIndexInfoResponse(
    List<OpenApiIndexInfoItem> items,
    int totalCount,
    int numOfRows
) {}
