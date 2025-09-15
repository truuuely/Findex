package com.findex.dto.syncjob;

import java.util.List;

public record OpenApiIndexDataResponse(
    List<OpenApiIndexDataItem> items,
    int totalCount,
    int numOfRows
) {}
