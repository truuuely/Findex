package com.findex.dto.dashboard;

import java.math.BigDecimal;

public record IndexPerformanceDto(
    Long indexInfoId,
    String indexClassification,
    String indexName,
    BigDecimal currentPrice,
    BigDecimal beforePrice,
    BigDecimal versus,
    BigDecimal fluctuationRate) {
}
