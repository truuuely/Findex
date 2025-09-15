package com.findex.dto.syncjob;

import java.util.List;

public record OpenApiIndexDataResponse(
    List<OpenApiIndexDataResponse> items,
    int totalCount,
    int numOfRows
) {}
