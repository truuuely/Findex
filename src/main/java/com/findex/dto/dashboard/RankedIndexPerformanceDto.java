package com.findex.dto.dashboard;

public record RankedIndexPerformanceDto(
    int rank,
    IndexPerformanceDto performance
) {}
