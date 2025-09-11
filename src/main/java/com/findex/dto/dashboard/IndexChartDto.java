package com.findex.dto.dashboard;

import com.findex.enums.ChartPeriodType;
import java.util.List;

public record IndexChartDto(
    Long indexInfoId,
    String indexClassification,
    String indexName,
    ChartPeriodType periodType,
    List<ChartDataPointDto> dataPoints,
    List<ChartDataPointDto> ma5DataPoints,
    List<ChartDataPointDto> ma20DataPoints
) {}